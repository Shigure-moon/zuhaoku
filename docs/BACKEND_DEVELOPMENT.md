# 租号酷后端开发文档

## 文档信息
- **版本**: v1.0
- **最后更新**: 2024
- **技术栈**: Spring Boot 3.2 + MyBatis-Plus + Redis
- **目标**: 提供完整的后端开发指南

---

## 一、技术栈与工具

### 1.1 核心框架
- **Spring Boot 3.2+**: 主框架
- **Spring Cloud Gateway**: API 网关（限流、鉴权、路由）
- **Spring Security**: 安全框架
- **MyBatis-Plus 3.5+**: ORM 框架
- **Redisson**: 分布式锁和缓存

### 1.2 数据存储
- **MySQL 8.0+**: 主数据库（主从架构）
- **Redis 7.0+**: 缓存、分布式锁、会话管理
- **MinIO**: 对象存储（截图、录屏）

### 1.3 工具库
- **Lombok**: 简化 Java 代码
- **Hutool**: Java 工具类库
- **MapStruct**: 对象映射
- **Jackson**: JSON 处理

### 1.4 监控与工具
- **Sentinel**: 限流熔断
- **Prometheus**: 指标收集
- **Swagger/OpenAPI**: API 文档

---

## 二、项目结构

```
backend/
├── zhk-common/              # 公共模块
│   ├── zhk-common-core/    # 核心工具类
│   ├── zhk-common-security/# 安全相关
│   └── zhk-common-web/     # Web 相关
├── zhk-gateway/            # API 网关
│   └── src/main/java/com/zhk/gateway/
│       ├── config/         # 网关配置
│       ├── filter/         # 过滤器
│       └── GatewayApplication.java
├── zhk-monolith/           # 业务聚合层（单体应用）
│   ├── zhk-user/           # 用户服务模块
│   │   ├── controller/     # 控制器
│   │   ├── service/        # 服务层
│   │   ├── mapper/         # Mapper 接口
│   │   ├── entity/         # 实体类
│   │   └── dto/            # 数据传输对象
│   ├── zhk-asset/          # 资产服务模块
│   ├── zhk-order/          # 订单服务模块
│   ├── zhk-wallet/         # 钱包服务模块
│   └── zhk-risk/           # 风控服务模块
├── zhk-infrastructure/     # 基础设施模块
│   ├── zhk-database/       # 数据库配置
│   ├── zhk-redis/          # Redis 配置
│   └── zhk-minio/          # MinIO 配置
├── pom.xml                 # Maven 父 POM
└── README.md
```

---

## 三、环境搭建

### 3.1 前置要求
- **JDK**: >= 17
- **Maven**: >= 3.8.0
- **MySQL**: >= 8.0
- **Redis**: >= 7.0
- **MinIO**: >= 最新版本

### 3.2 数据库初始化

**执行 SQL 脚本**（参考 zuhaoku.md 中的数据模型）：

```sql
-- 创建数据库
CREATE DATABASE zhk_rental DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE zhk_rental;

-- 执行表结构脚本（见 zuhaoku.md 3.4 节）
```

### 3.3 配置文件

**application.yml**
```yaml
spring:
  application:
    name: zhk-rental-core
  profiles:
    active: dev
  
  # 数据源配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/zhk_rental?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.zhk.**.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# 应用配置
zhk:
  # JWT 配置
  jwt:
    secret: your-jwt-secret-key-min-256-bits
    expiration: 86400000 # 24小时
  
  # 加密配置
  encryption:
    algorithm: AES
    mode: GCM
    key: your-aes-key-32-bytes
  
  # 支付配置
  payment:
    wechat:
      app-id: your-wechat-app-id
      mch-id: your-mch-id
      api-key: your-api-key
    alipay:
      app-id: your-alipay-app-id
      private-key: your-private-key
      public-key: your-public-key
  
  # MinIO 配置
  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: zhk-evidence
```

### 3.4 启动项目

```bash
# 1. 克隆项目
git clone <repository-url>
cd zuhaoku/backend

# 2. 编译项目
mvn clean install

# 3. 启动网关
cd zhk-gateway
mvn spring-boot:run

# 4. 启动业务服务
cd ../zhk-monolith
mvn spring-boot:run
```

---

## 四、核心功能实现

### 4.1 统一响应格式

**zhk-common/zhk-common-web/src/main/java/com/zhk/common/web/Result.java**

```java
package com.zhk.common.web;

import lombok.Data;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = success();
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
```

### 4.2 JWT 认证

**zhk-common/zhk-common-security/src/main/java/com/zhk/common/security/JwtUtil.java**

```java
package com.zhk.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    
    @Value("${zhk.jwt.secret}")
    private String secret;
    
    @Value("${zhk.jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
```

### 4.3 账号加密服务

**zhk-monolith/zhk-asset/src/main/java/com/zhk/asset/service/EncryptionService.java**

```java
package com.zhk.asset.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    @Value("${zhk.encryption.key}")
    private String masterKey;

    /**
     * 加密账号密码
     */
    public String encrypt(String plaintext, String accountId) {
        try {
            // 使用账号ID生成独立密钥
            SecretKey key = generateKey(accountId);
            
            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            
            // 将 IV 和密文组合
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);
            
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密账号密码
     */
    public String decrypt(String ciphertext, String accountId) {
        try {
            SecretKey key = generateKey(accountId);
            
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 为每个账号生成独立密钥
     */
    private SecretKey generateKey(String accountId) {
        // 使用主密钥和账号ID生成派生密钥
        // 实际实现应使用 HKDF 或类似方法
        String derivedKey = masterKey + accountId;
        return new SecretKeySpec(derivedKey.getBytes(), ALGORITHM);
    }
}
```

### 4.4 订单服务（租期计时）

**zhk-monolith/zhk-order/src/main/java/com/zhk/order/service/TimerService.java**

```java
package com.zhk.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimerService {
    
    private final RedissonClient redissonClient;
    private final OrderService orderService;

    /**
     * 创建租期令牌（Redis TTL）
     */
    public void createLeaseToken(Long orderId, int durationMinutes) {
        String key = "lease:token:" + orderId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set("active", durationMinutes, TimeUnit.MINUTES);
        
        // 设置到期前5分钟提醒
        String reminderKey = "lease:reminder:" + orderId;
        RBucket<String> reminderBucket = redissonClient.getBucket(reminderKey);
        int reminderMinutes = Math.max(0, durationMinutes - 5);
        reminderBucket.set("remind", reminderMinutes, TimeUnit.MINUTES);
    }

    /**
     * 延长租期
     */
    public void extendLease(Long orderId, int additionalMinutes) {
        String key = "lease:token:" + orderId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        
        long remainingTTL = bucket.remainTimeToLive();
        if (remainingTTL > 0) {
            bucket.expire(remainingTTL + additionalMinutes * 60 * 1000, TimeUnit.MILLISECONDS);
        } else {
            bucket.set("active", additionalMinutes, TimeUnit.MINUTES);
        }
    }

    /**
     * 定时任务：检查租期到期
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void checkLeaseExpiration() {
        // 查询所有即将到期的订单
        // 发送提醒或自动回收
        log.info("检查租期到期...");
    }

    /**
     * 自动回收账号
     */
    public void autoReturnAccount(Long orderId) {
        try {
            // 1. 通知启动器强制踢号
            // 2. 修改账号密码
            // 3. 更新订单状态
            // 4. 释放账号
            orderService.returnAccount(orderId);
            log.info("订单 {} 自动回收成功", orderId);
        } catch (Exception e) {
            log.error("自动回收失败: orderId={}", orderId, e);
        }
    }
}
```

### 4.5 分布式锁（防止重复下单）

**zhk-monolith/zhk-order/src/main/java/com/zhk/order/service/OrderService.java**

```java
package com.zhk.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final RedissonClient redissonClient;
    private final OrderMapper orderMapper;

    /**
     * 创建订单（使用分布式锁）
     */
    public Order createOrder(CreateOrderDto dto) {
        String lockKey = "order:lock:" + dto.getAccountId() + ":" + dto.getTenantId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁，最多等待3秒，锁定10秒
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    // 检查是否已有进行中的订单
                    Order existingOrder = orderMapper.selectByAccountAndTenant(
                        dto.getAccountId(), 
                        dto.getTenantId()
                    );
                    
                    if (existingOrder != null && 
                        (existingOrder.getStatus() == OrderStatus.PAYING || 
                         existingOrder.getStatus() == OrderStatus.LEASING)) {
                        throw new BusinessException("该账号正在租赁中");
                    }
                    
                    // 创建订单
                    Order order = new Order();
                    // ... 设置订单属性
                    orderMapper.insert(order);
                    
                    return order;
                } finally {
                    lock.unlock();
                }
            } else {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取锁失败");
        }
    }
}
```

---

## 五、API 接口设计

### 5.1 RESTful 规范

- **GET**: 查询资源
- **POST**: 创建资源
- **PUT**: 更新资源（全量）
- **PATCH**: 更新资源（部分）
- **DELETE**: 删除资源

### 5.2 接口示例

**用户相关接口**

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody @Valid RegisterDTO dto) {
        UserVO user = userService.register(dto);
        return Result.success(user);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        LoginVO loginVO = userService.login(dto);
        return Result.success(loginVO);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public Result<UserVO> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        UserVO user = userService.getUserById(userId);
        return Result.success(user);
    }
}
```

**订单相关接口**

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    @PreAuthorize("hasRole('TENANT')")
    public Result<OrderVO> createOrder(@RequestBody @Valid CreateOrderDTO dto) {
        OrderVO order = orderService.createOrder(dto);
        return Result.success(order);
    }

    /**
     * 查询订单列表
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<OrderVO>> getOrderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) OrderStatus status) {
        PageResult<OrderVO> result = orderService.getOrderList(page, size, status);
        return Result.success(result);
    }

    /**
     * 续租
     */
    @PostMapping("/{orderId}/renew")
    @PreAuthorize("hasRole('TENANT')")
    public Result<OrderVO> renewOrder(
            @PathVariable Long orderId,
            @RequestBody @Valid RenewOrderDTO dto) {
        OrderVO order = orderService.renewOrder(orderId, dto.getDuration());
        return Result.success(order);
    }

    /**
     * 还号
     */
    @PostMapping("/{orderId}/return")
    @PreAuthorize("hasRole('TENANT')")
    public Result<Void> returnOrder(@PathVariable Long orderId) {
        orderService.returnOrder(orderId);
        return Result.success();
    }
}
```

---

## 六、开发规范

### 6.1 代码规范

#### 命名规范
- **类名**: PascalCase，如 `UserService`
- **方法名**: camelCase，如 `getUserById`
- **常量**: UPPER_SNAKE_CASE，如 `MAX_RETRY_COUNT`
- **包名**: 小写，如 `com.zhk.user.service`

#### 分层规范
```
Controller 层: 接收请求，参数校验，调用 Service
Service 层: 业务逻辑处理
Mapper 层: 数据库操作
Entity 层: 实体类（对应数据库表）
DTO 层: 数据传输对象（API 接口）
VO 层: 视图对象（返回给前端）
```

### 6.2 异常处理

**统一异常处理器**

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(400, "参数校验失败: " + message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "系统异常，请联系管理员");
    }
}
```

### 6.3 日志规范

```java
// 使用 SLF4J + Logback
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j  // Lombok 注解，自动生成 log 对象
public class OrderService {
    
    public void createOrder(CreateOrderDTO dto) {
        log.info("创建订单: accountId={}, tenantId={}", dto.getAccountId(), dto.getTenantId());
        
        try {
            // 业务逻辑
            log.debug("订单创建成功: orderId={}", order.getId());
        } catch (Exception e) {
            log.error("订单创建失败: accountId={}", dto.getAccountId(), e);
            throw e;
        }
    }
}
```

---

## 七、测试

### 7.1 单元测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderServiceTest {
    
    @Autowired
    private OrderService orderService;
    
    @MockBean
    private OrderMapper orderMapper;
    
    @Test
    void testCreateOrder() {
        // Given
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setAccountId(1L);
        dto.setTenantId(2L);
        
        // When
        Order order = orderService.createOrder(dto);
        
        // Then
        assertNotNull(order);
        assertEquals(OrderStatus.PAYING, order.getStatus());
    }
}
```

### 7.2 集成测试

使用 Testcontainers 进行集成测试：

```java
@SpringBootTest
@Testcontainers
class OrderIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.0")
            .withExposedPorts(6379);
    
    // 测试代码
}
```

---

## 八、部署

### 8.1 Docker 部署

**Dockerfile**
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

### 8.2 Docker Compose

**docker-compose.yml**
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: zhk_rental
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7.0
    ports:
      - "6379:6379"

  minio:
    image: minio/minio
    command: server /data
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"

  backend:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - minio
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/zhk_rental
      SPRING_DATA_REDIS_HOST: redis

volumes:
  mysql_data:
```

---

## 九、监控与运维

### 9.1 健康检查

```java
@RestController
public class HealthController {
    
    @GetMapping("/actuator/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
}
```

### 9.2 日志收集

配置 Logback 输出到 ELK：

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="ELK" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>logstash:5044</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="ELK"/>
    </root>
</configuration>
```

---

## 十、参考资源

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus 文档](https://baomidou.com/)
- [Redisson 文档](https://github.com/redisson/redisson)
- [Spring Security 文档](https://spring.io/projects/spring-security)

---

**文档维护**: shigure
**最后更新**: 2025/11/18

