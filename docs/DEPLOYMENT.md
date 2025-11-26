# 部署架构文档

## 文档信息
- **版本**: v1.0
- **最后更新**: 2025/11/18
- **维护者**: shigure

---

## 一、部署架构概述

部署架构采用**渐进式演进**策略，从单机到集群平滑过渡。

详见架构图：`../diagrams/ZHK_C4_Deployment.puml`

---

## 二、部署阶段

### 2.1 阶段一：MVP 阶段（日订单 < 1k）

#### 部署方式
- Docker Compose 单机部署

#### 容器配置
```yaml
services:
  zhk-app:
    image: zhk-backend:latest
    resources:
      limits:
        cpus: '2'
        memory: 4G
  
  mysql:
    image: mysql:8.0
    resources:
      limits:
        cpus: '2'
        memory: 4G
  
  redis:
    image: redis:7.0
    resources:
      limits:
        cpus: '1'
        memory: 2G
  
  minio:
    image: minio/minio
    resources:
      limits:
        cpus: '1'
        memory: 2G
  
  nginx:
    image: nginx:alpine
    resources:
      limits:
        cpus: '1'
        memory: 512M
```

### 2.2 阶段二：成长阶段（日订单 1k - 5k）

#### 部署方式
- Docker Swarm / Kubernetes 单集群

#### 架构升级
- MySQL 主从复制（1 主 1 从）
- Redis Sentinel（1 主 2 从）
- 业务层多实例（2-3 个 Pod）
- Nginx 负载均衡

### 2.3 阶段三：成熟阶段（日订单 > 5k）

#### 部署方式
- Kubernetes 多集群

#### 架构升级
- MySQL 读写分离（1 主 N 从，按业务分库）
- Redis Cluster（3 主 3 从）
- 业务层微服务拆分（按模块拆分）
- 消息队列（RabbitMQ/Kafka）引入
- 服务网格（Istio）用于服务治理

---

## 三、灰度发布策略

### 3.1 用户灰度
- 新功能先对 5% 白名单用户开放
- 稳定后逐步扩大范围（5% → 20% → 50% → 100%）

### 3.2 地域灰度
- 新功能先在单个地域（如华东）上线
- 稳定后推广到其他地域

### 3.3 功能开关
- 使用 Feature Flag 机制
- 支持快速回滚
- 配置中心管理（如 Nacos）

### 3.4 监控告警
- 实时监控错误率、响应时间
- 异常自动回滚
- 告警通知（钉钉/企业微信）

---

## 四、监控与可观测性

### 4.1 指标监控

#### Prometheus + Grafana
- **业务指标**: 订单量、支付成功率、用户活跃度
- **技术指标**: CPU、内存、QPS、错误率
- **自定义指标**: 租期到期数、自动回收数

### 4.2 日志聚合

#### ELK Stack
- **Elasticsearch**: 日志存储
- **Logstash**: 日志收集和解析
- **Kibana**: 日志分析和可视化

### 4.3 链路追踪

#### SkyWalking / Jaeger
- **分布式追踪**: 追踪请求在服务间的流转
- **性能分析**: 分析服务调用链性能
- **问题定位**: 快速定位性能瓶颈

### 4.4 告警机制
- **AlertManager**: 告警管理
- **通知渠道**: 钉钉、企业微信、邮件
- **告警规则**: 基于 Prometheus 指标

---

## 五、Docker 部署

### 5.1 Dockerfile

#### 后端 Dockerfile
```dockerfile
FROM maven:3.8-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=builder /app/zhk-monolith/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 前端 Dockerfile
```dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 5.2 Docker Compose

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: zhk_rental
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - zhk-network

  redis:
    image: redis:7.0
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - zhk-network

  minio:
    image: minio/minio
    command: server /data
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data
    networks:
      - zhk-network

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/zhk_rental
      SPRING_DATA_REDIS_HOST: redis
      MINIO_ENDPOINT: http://minio:9000
    depends_on:
      - mysql
      - redis
      - minio
    networks:
      - zhk-network

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - zhk-network

volumes:
  mysql_data:
  redis_data:
  minio_data:

networks:
  zhk-network:
    driver: bridge
```

---

## 六、Kubernetes 部署

### 6.1 命名空间
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: zhk-production
```

### 6.2 后端 Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: zhk-backend
  namespace: zhk-production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: zhk-backend
  template:
    metadata:
      labels:
        app: zhk-backend
    spec:
      containers:
      - name: backend
        image: zhk-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1"
          limits:
            memory: "4Gi"
            cpu: "2"
```

### 6.3 Service
```yaml
apiVersion: v1
kind: Service
metadata:
  name: zhk-backend-service
  namespace: zhk-production
spec:
  selector:
    app: zhk-backend
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
```

### 6.4 Ingress
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: zhk-ingress
  namespace: zhk-production
spec:
  rules:
  - host: api.zuhaoku.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: zhk-backend-service
            port:
              number: 8080
```

---

## 七、数据库部署

### 7.1 MySQL 主从配置

#### 主库配置
```ini
[mysqld]
server-id = 1
log-bin = mysql-bin
binlog-format = ROW
```

#### 从库配置
```ini
[mysqld]
server-id = 2
relay-log = mysql-relay-bin
read-only = 1
```

### 7.2 Redis Cluster 配置

```yaml
# redis-cluster.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-cluster-config
data:
  redis.conf: |
    cluster-enabled yes
    cluster-config-file nodes.conf
    cluster-node-timeout 5000
    appendonly yes
```

---

## 八、CI/CD 流程

### 8.1 GitLab CI/CD

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - deploy

build:
  stage: build
  script:
    - mvn clean package -DskipTests
    - docker build -t zhk-backend:$CI_COMMIT_SHA .
    - docker push zhk-backend:$CI_COMMIT_SHA

test:
  stage: test
  script:
    - mvn test

deploy:
  stage: deploy
  script:
    - kubectl set image deployment/zhk-backend backend=zhk-backend:$CI_COMMIT_SHA
  only:
    - main
```

---

## 九、容灾与备份

### 9.1 数据库备份
- **全量备份**: 每日凌晨执行
- **增量备份**: 每小时执行
- **备份保留**: 保留 30 天

### 9.2 应用备份
- **配置备份**: 配置文件版本控制
- **代码备份**: Git 仓库备份

### 9.3 灾难恢复
- **RTO**: < 30 分钟（恢复时间目标）
- **RPO**: < 5 分钟（恢复点目标）
- **演练**: 每季度执行灾难恢复演练

---

## 十、参考资源

- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [Docker 官方文档](https://docs.docker.com/)
- [Prometheus 文档](https://prometheus.io/docs/)

---

**文档维护**: shigure  
**最后更新**: 2025/11/18

