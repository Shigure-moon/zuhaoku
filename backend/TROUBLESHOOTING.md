# 后端启动问题排查

## 当前问题

1. **MyBatis-Plus 兼容性问题**：`Invalid value type for attribute 'factoryBeanObjectType'`
2. **Lombok 未生效**：找不到 getter/setter 方法

## 已尝试的解决方案

1. ✅ 升级 MyBatis-Plus 版本：3.5.5 → 3.5.9
2. ✅ 升级 Lombok 版本：1.18.30 → 1.18.34
3. ✅ 添加 Lombok 依赖到 zhk-user 模块
4. ✅ 修复 MapperScan 配置

## 建议的解决方案

### 方案一：降级 Java 版本（推荐）

Java 21 可能与某些库不完全兼容，建议使用 JDK 17：

```bash
# 安装 JDK 17
sudo apt install openjdk-17-jdk

# 设置 JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# 验证
java -version
```

### 方案二：使用最新版本的依赖

```xml
<lombok.version>1.18.36</lombok.version>
<mybatis-plus.version>3.5.10</mybatis-plus.version>
```

### 方案三：手动添加 getter/setter（临时方案）

如果 Lombok 仍然不工作，可以手动添加 getter/setter 方法。

