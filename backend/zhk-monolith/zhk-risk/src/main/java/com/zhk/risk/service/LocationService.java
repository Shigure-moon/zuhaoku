package com.zhk.risk.service;

import com.zhk.risk.dto.LocationInfo;
import com.zhk.risk.dto.RiskCheckResult;
import com.zhk.risk.entity.UserCommonLocation;
import com.zhk.risk.entity.UserLoginRecord;
import com.zhk.risk.mapper.UserCommonLocationMapper;
import com.zhk.risk.mapper.UserLoginRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 地理位置服务
 * 负责IP地理位置查询、异地登录检测
 *
 * @author shigure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final UserLoginRecordMapper loginRecordMapper;
    private final UserCommonLocationMapper commonLocationMapper;

    // 异地登录距离阈值（公里）
    private static final double REMOTE_LOGIN_THRESHOLD = 200.0;
    // 多地登录时间窗口（分钟）
    private static final int MULTI_LOCATION_WINDOW = 5;

    /**
     * 根据IP地址获取地理位置信息
     * 注意：这里使用简化实现，实际应使用GeoIP2数据库或第三方API
     *
     * @param ipAddress IP地址
     * @return 地理位置信息
     */
    public LocationInfo getLocationByIp(String ipAddress) {
        // TODO: 集成GeoIP2数据库或第三方API（如高德地图、百度地图API）
        // 这里返回模拟数据
        LocationInfo location = new LocationInfo();
        
        // 处理IPv6本地回环地址，转换为IPv4格式
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = "127.0.0.1";
        }
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }
        
        // 简化实现：根据IP段判断（实际应使用GeoIP2）
        if (ipAddress.startsWith("127.") || ipAddress.startsWith("192.168.") || 
            ipAddress.startsWith("10.") || ipAddress.startsWith("172.16.") ||
            ipAddress.startsWith("172.17.") || ipAddress.startsWith("172.18.") ||
            ipAddress.startsWith("172.19.") || ipAddress.startsWith("172.20.") ||
            ipAddress.startsWith("172.21.") || ipAddress.startsWith("172.22.") ||
            ipAddress.startsWith("172.23.") || ipAddress.startsWith("172.24.") ||
            ipAddress.startsWith("172.25.") || ipAddress.startsWith("172.26.") ||
            ipAddress.startsWith("172.27.") || ipAddress.startsWith("172.28.") ||
            ipAddress.startsWith("172.29.") || ipAddress.startsWith("172.30.") ||
            ipAddress.startsWith("172.31.") || ipAddress.startsWith("169.254.")) {
            // 内网IP，默认返回本地
            location.setCountry("中国");
            location.setProvince("未知");
            location.setCity("本地");
            location.setLatitude(new BigDecimal("39.9042")); // 北京
            location.setLongitude(new BigDecimal("116.4074"));
        } else {
            // 外网IP，这里简化处理，实际应查询GeoIP2数据库
            location.setCountry("中国");
            location.setProvince("未知");
            location.setCity("未知");
            location.setLatitude(new BigDecimal("39.9042")); // 默认北京
            location.setLongitude(new BigDecimal("116.4074"));
        }
        
        log.debug("IP地理位置查询: ip={}, location={}", ipAddress, location);
        return location;
    }

    /**
     * 记录用户登录
     *
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param deviceFingerprint 设备指纹
     * @return 登录记录
     */
    public UserLoginRecord recordLogin(Long userId, String ipAddress, String userAgent, String deviceFingerprint) {
        // 获取IP地理位置
        LocationInfo location = getLocationByIp(ipAddress);

        // 创建登录记录
        UserLoginRecord record = new UserLoginRecord();
        record.setUserId(userId);
        record.setIpAddress(ipAddress);
        record.setCountry(location.getCountry());
        record.setProvince(location.getProvince());
        record.setCity(location.getCity());
        record.setLatitude(location.getLatitude());
        record.setLongitude(location.getLongitude());
        record.setUserAgent(userAgent);
        record.setDeviceFingerprint(deviceFingerprint);
        record.setLoginTime(LocalDateTime.now());
        record.setIsSuspicious(0);
        record.setRiskLevel(0);

        // 检查异地登录
        RiskCheckResult riskCheck = checkRemoteLogin(userId, location);
        if (!riskCheck.getPassed()) {
            record.setIsSuspicious(1);
            record.setRiskLevel(riskCheck.getRiskLevel());
        }

        // 保存登录记录
        loginRecordMapper.insert(record);

        // 更新常用登录地
        updateCommonLocation(userId, location);

        return record;
    }

    /**
     * 检查异地登录
     *
     * @param userId 用户ID
     * @param currentLocation 当前登录位置
     * @return 风控检查结果
     */
    public RiskCheckResult checkRemoteLogin(Long userId, LocationInfo currentLocation) {
        RiskCheckResult result = new RiskCheckResult();
        result.setPassed(true);
        result.setRiskLevel(0);
        result.setNeedFaceVerification(false);
        result.setNeedFreezeAccount(false);

        // 查询用户常用登录地
        LambdaQueryWrapper<UserCommonLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCommonLocation::getUserId, userId);
        wrapper.orderByDesc(UserCommonLocation::getLoginCount);
        wrapper.last("LIMIT 1");
        UserCommonLocation commonLocation = commonLocationMapper.selectOne(wrapper);

        if (commonLocation == null) {
            // 首次登录，记录为常用登录地
            log.info("用户首次登录，记录常用登录地: userId={}", userId);
            return result;
        }

        // 计算距离
        double distance = calculateDistance(
                commonLocation.getLatitude().doubleValue(),
                commonLocation.getLongitude().doubleValue(),
                currentLocation.getLatitude().doubleValue(),
                currentLocation.getLongitude().doubleValue()
        );
        result.setDistance(distance);

        log.info("异地登录检测: userId={}, distance={}km, threshold={}km", userId, distance, REMOTE_LOGIN_THRESHOLD);

        // 检查是否超过阈值
        if (distance > REMOTE_LOGIN_THRESHOLD) {
            result.setPassed(false);
            result.setRiskLevel(2); // 中等风险
            result.setNeedFaceVerification(true);
            result.setReason(String.format("检测到异地登录，距离常用登录地 %.2f 公里", distance));
            log.warn("检测到异地登录: userId={}, distance={}km", userId, distance);
        }

        // 检查多地登录（5分钟内不同IP登录）
        boolean multiLocation = checkMultiLocationLogin(userId);
        if (multiLocation) {
            result.setPassed(false);
            result.setRiskLevel(3); // 高风险
            result.setNeedFreezeAccount(true);
            result.setReason("检测到多地同时登录，账号已冻结");
            log.error("检测到多地登录，冻结账号: userId={}", userId);
        }

        return result;
    }

    /**
     * 检查多地登录
     *
     * @param userId 用户ID
     * @return 是否多地登录
     */
    private boolean checkMultiLocationLogin(Long userId) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(MULTI_LOCATION_WINDOW);
        
        LambdaQueryWrapper<UserLoginRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLoginRecord::getUserId, userId);
        wrapper.ge(UserLoginRecord::getLoginTime, fiveMinutesAgo);
        wrapper.select(UserLoginRecord::getIpAddress);
        
        List<UserLoginRecord> recentLogins = loginRecordMapper.selectList(wrapper);
        
        // 统计不同IP数量
        long distinctIpCount = recentLogins.stream()
                .map(UserLoginRecord::getIpAddress)
                .distinct()
                .count();
        
        return distinctIpCount > 1;
    }

    /**
     * 更新用户常用登录地
     *
     * @param userId 用户ID
     * @param location 登录位置
     */
    private void updateCommonLocation(Long userId, LocationInfo location) {
        // 查询是否已存在该位置的记录
        LambdaQueryWrapper<UserCommonLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCommonLocation::getUserId, userId);
        wrapper.eq(UserCommonLocation::getCity, location.getCity());
        if (location.getLatitude() != null && location.getLongitude() != null) {
            // 使用经纬度精确匹配（允许小范围误差）
            wrapper.apply("ABS(latitude - {0}) < 0.01 AND ABS(longitude - {1}) < 0.01",
                    location.getLatitude(), location.getLongitude());
        }
        
        UserCommonLocation commonLocation = commonLocationMapper.selectOne(wrapper);
        
        if (commonLocation != null) {
            // 更新登录次数和时间
            commonLocation.setLoginCount(commonLocation.getLoginCount() + 1);
            commonLocation.setLastLoginTime(LocalDateTime.now());
            commonLocationMapper.updateById(commonLocation);
        } else {
            // 创建新记录
            commonLocation = new UserCommonLocation();
            commonLocation.setUserId(userId);
            commonLocation.setCountry(location.getCountry());
            commonLocation.setProvince(location.getProvince());
            commonLocation.setCity(location.getCity());
            commonLocation.setLatitude(location.getLatitude());
            commonLocation.setLongitude(location.getLongitude());
            commonLocation.setLoginCount(1);
            commonLocation.setFirstLoginTime(LocalDateTime.now());
            commonLocation.setLastLoginTime(LocalDateTime.now());
            commonLocationMapper.insert(commonLocation);
        }
    }

    /**
     * 计算两点之间的距离（使用Haversine公式）
     *
     * @param lat1 纬度1
     * @param lon1 经度1
     * @param lat2 纬度2
     * @param lon2 经度2
     * @return 距离（公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

