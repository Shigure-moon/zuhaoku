# 支付宝接口接入文档

## 一、准备工作

### 1.1 支付宝开放平台配置

根据 `START_SERVICES.md` 中的配置信息：

- **接口加签方式**: 已设置（密钥方式，RSA2算法）
- **接口内容加密方式**: 已设置（AES密钥：`lE4SjgSUUKi5tmrfKB3A3g==`）
- **支付宝网关地址**: `https://openapi.alipay.com/gateway.do`
- **应用网关**: 未设置（需要配置用于接收异步通知）

### 1.2 密钥文件

密钥文件已生成在：`backend/scripts/alipay-keys/`

- **应用私钥**: `alipay_app_private_key_pkcs8.pem`
- **应用公钥**: `alipay_app_public_key.pem`
- **应用公钥字符串**: 已上传到支付宝开放平台

### 1.3 需要配置的信息

1. **应用ID (APP_ID)**: 从支付宝开放平台获取
2. **应用私钥**: 使用 `alipay_app_private_key_pkcs8.pem` 文件内容
3. **支付宝公钥**: 从支付宝开放平台下载（上传应用公钥后获取）
4. **接口内容加密密钥**: `lE4SjgSUUKi5tmrfKB3A3g==`
5. **应用网关**: 配置用于接收异步通知的URL（如：`https://your-domain.com/api/v1/payments/alipay/notify`）

---

## 二、Maven 依赖

### 2.1 添加支付宝 SDK

在 `zhk-order/pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.alipay.sdk</groupId>
    <artifactId>alipay-sdk-java</artifactId>
    <version>4.40.0.ALL</version>
</dependency>
```

### 2.2 完整依赖示例

```xml
<dependencies>
    <!-- 支付宝 SDK -->
    <dependency>
        <groupId>com.alipay.sdk</groupId>
        <artifactId>alipay-sdk-java</artifactId>
        <version>4.40.0.ALL</version>
    </dependency>
    
    <!-- 其他依赖... -->
</dependencies>
```

---

## 三、配置文件

### 3.1 application-dev.yml

在 `zhk-order/src/main/resources/application-dev.yml` 中添加：

```yaml
zhk:
  alipay:
    app-id: YOUR_APP_ID  # 从支付宝开放平台获取
    private-key: |
      -----BEGIN PRIVATE KEY-----
      ...（私钥内容）...
      -----END PRIVATE KEY-----
    alipay-public-key: |
      -----BEGIN PUBLIC KEY-----
      ...（支付宝公钥内容）...
      -----END PUBLIC KEY-----
    gateway-url: https://openapi.alipay.com/gateway.do
    encrypt-key: lE4SjgSUUKi5tmrfKB3A3g==  # 接口内容加密密钥
    notify-url: http://localhost:8081/api/v1/payments/alipay/notify  # 异步通知地址
    return-url: http://localhost:3000/orders  # 同步跳转地址
    sign-type: RSA2  # 签名算法
    charset: UTF-8
```

### 3.2 生产环境配置

生产环境建议使用环境变量：

```bash
export ALIPAY_APP_ID=YOUR_APP_ID
export ALIPAY_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
export ALIPAY_PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"
export ALIPAY_ENCRYPT_KEY="lE4SjgSUUKi5tmrfKB3A3g=="
export ALIPAY_NOTIFY_URL="https://your-domain.com/api/v1/payments/alipay/notify"
```

---

## 四、代码实现

### 4.1 配置类

**AlipayProperties.java**

```java
package com.zhk.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zhk.alipay")
public class AlipayProperties {
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";
    private String encryptKey;
    private String notifyUrl;
    private String returnUrl;
    private String signType = "RSA2";
    private String charset = "UTF-8";
}
```

### 4.2 支付宝客户端配置

**AlipayClientConfig.java**

```java
package com.zhk.order.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AlipayClientConfig {
    
    private final AlipayProperties alipayProperties;
    
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
            alipayProperties.getGatewayUrl(),
            alipayProperties.getAppId(),
            alipayProperties.getPrivateKey(),
            "json",
            alipayProperties.getCharset(),
            alipayProperties.getAlipayPublicKey(),
            alipayProperties.getSignType()
        );
    }
}
```

### 4.3 支付服务实现

**AlipayPaymentService.java**

```java
package com.zhk.order.service;

import com.zhk.order.dto.CreatePaymentDTO;
import com.zhk.order.dto.PaymentVO;

public interface AlipayPaymentService {
    /**
     * 创建支付宝支付
     */
    PaymentVO createPayment(Long userId, CreatePaymentDTO dto);
    
    /**
     * 处理支付回调
     */
    void handleNotify(java.util.Map<String, String> params);
    
    /**
     * 查询支付状态
     */
    PaymentVO queryPaymentStatus(String outTradeNo);
}
```

**AlipayPaymentServiceImpl.java**

```java
package com.zhk.order.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.zhk.common.web.BusinessException;
import com.zhk.order.config.AlipayProperties;
import com.zhk.order.dto.CreatePaymentDTO;
import com.zhk.order.dto.PaymentVO;
import com.zhk.order.entity.LeaseOrder;
import com.zhk.order.entity.PaymentRecord;
import com.zhk.order.mapper.LeaseOrderMapper;
import com.zhk.order.mapper.PaymentRecordMapper;
import com.zhk.order.service.AlipayPaymentService;
import com.zhk.order.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayPaymentServiceImpl implements AlipayPaymentService {
    
    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProperties;
    private final PaymentRecordMapper paymentMapper;
    private final LeaseOrderMapper orderMapper;
    private final PaymentService paymentService;
    
    @Override
    @Transactional
    public PaymentVO createPayment(Long userId, CreatePaymentDTO dto) {
        // 查询订单
        LeaseOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        
        // 验证权限
        if (!order.getTenantUid().equals(userId)) {
            throw new BusinessException(403, "无权限操作此订单");
        }
        
        // 检查订单状态
        if (!"paying".equals(order.getStatus())) {
            throw new BusinessException(400, "订单状态不正确，无法支付");
        }
        
        // 查询或创建支付记录
        PaymentRecord payment = getOrCreatePayment(dto.getOrderId(), order);
        
        try {
            // 创建支付请求
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            
            // 设置业务参数
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(payment.getTransactionId());
            model.setTotalAmount(order.getAmount().add(order.getDeposit()).toString());
            model.setSubject("租号酷-账号租赁");
            model.setBody("订单号: " + order.getId() + ", 账号: " + order.getAccountId());
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            
            request.setBizModel(model);
            request.setNotifyUrl(alipayProperties.getNotifyUrl());
            request.setReturnUrl(alipayProperties.getReturnUrl());
            
            // 调用接口
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            
            if (response.isSuccess()) {
                PaymentVO vo = new PaymentVO();
                vo.setId(payment.getId());
                vo.setOrderId(payment.getOrderId());
                vo.setPaymentType("alipay");
                vo.setAmount(payment.getAmount());
                vo.setStatus("pending");
                vo.setTransactionId(payment.getTransactionId());
                vo.setPaymentUrl(response.getBody()); // 返回支付表单HTML
                return vo;
            } else {
                log.error("支付宝支付创建失败: {}", response.getSubMsg());
                throw new BusinessException(500, "支付创建失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝接口失败", e);
            throw new BusinessException(500, "支付创建失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void handleNotify(Map<String, String> params) {
        log.info("收到支付宝支付回调: {}", params);
        
        // 验证签名
        if (!verifySign(params)) {
            log.error("支付宝回调签名验证失败");
            throw new BusinessException(400, "签名验证失败");
        }
        
        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        
        // 查询支付记录
        PaymentRecord payment = paymentMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getTransactionId, outTradeNo)
        );
        
        if (payment == null) {
            log.error("支付记录不存在: {}", outTradeNo);
            return;
        }
        
        // 处理支付结果
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            if ("pending".equals(payment.getStatus())) {
                payment.setStatus("success");
                payment.setPaidAt(LocalDateTime.now());
                paymentMapper.updateById(payment);
                
                // 更新订单状态
                paymentService.onPaymentSuccess(payment.getId());
                log.info("支付成功: orderId={}, paymentId={}", payment.getOrderId(), payment.getId());
            }
        } else if ("TRADE_CLOSED".equals(tradeStatus)) {
            payment.setStatus("failed");
            paymentMapper.updateById(payment);
            log.info("支付关闭: orderId={}, paymentId={}", payment.getOrderId(), payment.getId());
        }
    }
    
    @Override
    public PaymentVO queryPaymentStatus(String outTradeNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\"}");
            
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            
            if (response.isSuccess()) {
                PaymentRecord payment = paymentMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getTransactionId, outTradeNo)
                );
                
                if (payment != null) {
                    PaymentVO vo = new PaymentVO();
                    vo.setId(payment.getId());
                    vo.setOrderId(payment.getOrderId());
                    vo.setPaymentType(payment.getPaymentType());
                    vo.setAmount(payment.getAmount());
                    vo.setStatus(mapTradeStatus(response.getTradeStatus()));
                    vo.setTransactionId(payment.getTransactionId());
                    return vo;
                }
            }
        } catch (AlipayApiException e) {
            log.error("查询支付状态失败", e);
        }
        
        return null;
    }
    
    /**
     * 获取或创建支付记录
     */
    private PaymentRecord getOrCreatePayment(Long orderId, LeaseOrder order) {
        PaymentRecord existing = paymentMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId)
                .eq(PaymentRecord::getStatus, "pending")
        );
        
        if (existing != null) {
            return existing;
        }
        
        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(orderId);
        payment.setPaymentType("alipay");
        payment.setAmount(order.getAmount().add(order.getDeposit()));
        payment.setStatus("pending");
        payment.setTransactionId("ALIPAY" + System.currentTimeMillis() + orderId);
        paymentMapper.insert(payment);
        
        return payment;
    }
    
    /**
     * 验证签名
     */
    private boolean verifySign(Map<String, String> params) {
        try {
            return com.alipay.api.internal.util.AlipaySignature.rsaCheckV1(
                params,
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getCharset(),
                alipayProperties.getSignType()
            );
        } catch (Exception e) {
            log.error("签名验证异常", e);
            return false;
        }
    }
    
    /**
     * 映射交易状态
     */
    private String mapTradeStatus(String tradeStatus) {
        switch (tradeStatus) {
            case "WAIT_BUYER_PAY":
                return "pending";
            case "TRADE_SUCCESS":
            case "TRADE_FINISHED":
                return "success";
            case "TRADE_CLOSED":
                return "failed";
            default:
                return "pending";
        }
    }
}
```

### 4.4 支付回调控制器

**AlipayNotifyController.java**

```java
package com.zhk.order.controller;

import com.zhk.order.service.AlipayPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/alipay")
@RequiredArgsConstructor
public class AlipayNotifyController {
    
    private final AlipayPaymentService alipayPaymentService;
    
    /**
     * 支付宝异步通知
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            
            for (String name : requestParams.keySet()) {
                String[] values = requestParams.get(name);
                String valueStr = "";
                for (int i = 0; i < values.length; i++) {
                    valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
                }
                params.put(name, valueStr);
            }
            
            alipayPaymentService.handleNotify(params);
            
            return "success";
        } catch (Exception e) {
            log.error("处理支付宝回调失败", e);
            return "fail";
        }
    }
    
    /**
     * 支付宝同步跳转
     */
    @GetMapping("/return")
    public String returnUrl(HttpServletRequest request) {
        // 处理同步跳转，通常重定向到前端页面
        return "redirect:/orders";
    }
}
```

---

## 五、更新 PaymentServiceImpl

修改 `PaymentServiceImpl.java`，集成支付宝服务：

```java
// 在 PaymentServiceImpl 中注入 AlipayPaymentService
private final AlipayPaymentService alipayPaymentService;

@Override
public PaymentVO createPayment(Long userId, CreatePaymentDTO dto) {
    // ... 验证逻辑 ...
    
    if ("alipay".equals(dto.getPaymentType())) {
        return alipayPaymentService.createPayment(userId, dto);
    } else if ("wechat".equals(dto.getPaymentType())) {
        // 微信支付逻辑
    }
    
    throw new BusinessException(400, "不支持的支付方式");
}
```

---

## 六、测试

### 6.1 沙箱环境测试

1. 登录支付宝开放平台
2. 进入 **开发助手** -> **沙箱环境**
3. 获取沙箱账号信息
4. 修改配置使用沙箱网关：`https://openapi.alipaydev.com/gateway.do`

### 6.2 测试流程

1. 创建订单
2. 调用支付接口
3. 使用沙箱账号完成支付
4. 验证回调处理
5. 验证订单状态更新

---

## 七、注意事项

### 7.1 安全

1. **私钥保护**: 不要将私钥提交到代码仓库
2. **签名验证**: 所有回调必须验证签名
3. **幂等性**: 支付回调可能重复，需要保证幂等性
4. **HTTPS**: 生产环境必须使用HTTPS

### 7.2 异步通知

1. **应用网关**: 必须配置公网可访问的URL
2. **超时处理**: 支付宝会在24小时内多次通知
3. **响应格式**: 必须返回 "success" 或 "fail"

### 7.3 接口内容加密

如果启用了接口内容加密（AES），需要在请求和响应中处理加密/解密。

---

## 八、参考文档

- [支付宝开放平台](https://open.alipay.com/)
- [支付宝开发文档](https://opendocs.alipay.com/)
- [Java SDK 文档](https://opendocs.alipay.com/apis/api_1/alipay.trade.page.pay)
- [密钥与证书格式说明](https://opendocs.alipay.com/common/02kkv7)

---

**维护者**: shigure  
**最后更新**: 2025/11/20

