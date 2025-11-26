-- 风控服务相关表
-- 用于异地登录检测、异常行为识别、黑名单管理

-- 用户登录记录表
CREATE TABLE IF NOT EXISTS user_login_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  ip_address VARCHAR(50) NOT NULL COMMENT '登录IP地址',
  country VARCHAR(50) COMMENT '国家',
  province VARCHAR(50) COMMENT '省份',
  city VARCHAR(50) COMMENT '城市',
  latitude DECIMAL(10, 7) COMMENT '纬度',
  longitude DECIMAL(10, 7) COMMENT '经度',
  device_fingerprint VARCHAR(128) COMMENT '设备指纹',
  user_agent VARCHAR(500) COMMENT '用户代理',
  login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  is_suspicious TINYINT(1) DEFAULT 0 COMMENT '是否可疑：1-是 0-否',
  risk_level TINYINT DEFAULT 0 COMMENT '风险等级：0-正常 1-低 2-中 3-高',
  FOREIGN KEY (user_id) REFERENCES user(id),
  INDEX idx_user_id (user_id),
  INDEX idx_ip_address (ip_address),
  INDEX idx_login_time (login_time),
  INDEX idx_user_time (user_id, login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录记录表';

-- 异常行为记录表
CREATE TABLE IF NOT EXISTS abnormal_behavior (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT COMMENT '用户ID（可为空，如系统异常）',
  behavior_type VARCHAR(50) NOT NULL COMMENT '行为类型：FREQUENT_CANCEL/PAYMENT_FAILURE/ACCOUNT_ABUSE/MULTI_LOCATION_LOGIN等',
  description TEXT COMMENT '行为描述',
  risk_score INT DEFAULT 0 COMMENT '风险评分：0-100',
  resource_type VARCHAR(50) COMMENT '资源类型：ORDER/ACCOUNT/PAYMENT等',
  resource_id BIGINT COMMENT '资源ID',
  ip_address VARCHAR(50) COMMENT 'IP地址',
  device_fingerprint VARCHAR(128) COMMENT '设备指纹',
  status TINYINT DEFAULT 0 COMMENT '处理状态：0-待处理 1-已处理 2-已忽略',
  handled_by BIGINT COMMENT '处理人ID',
  handled_at TIMESTAMP NULL COMMENT '处理时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (user_id) REFERENCES user(id),
  INDEX idx_user_id (user_id),
  INDEX idx_behavior_type (behavior_type),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常行为记录表';

-- 黑名单表
CREATE TABLE IF NOT EXISTS blacklist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type VARCHAR(20) NOT NULL COMMENT '类型：IP/DEVICE/PHONE/USER',
  value VARCHAR(255) NOT NULL COMMENT '黑名单值（IP地址/设备指纹/手机号/用户ID）',
  reason TEXT COMMENT '加入黑名单原因',
  risk_level TINYINT DEFAULT 2 COMMENT '风险等级：1-低 2-中 3-高',
  status TINYINT DEFAULT 1 COMMENT '状态：1-生效 0-失效',
  created_by BIGINT COMMENT '创建人ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  expires_at TIMESTAMP NULL COMMENT '过期时间（NULL表示永久）',
  INDEX idx_type_value (type, value),
  INDEX idx_status (status),
  INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单表';

-- 用户常用登录地表
CREATE TABLE IF NOT EXISTS user_common_location (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  country VARCHAR(50) COMMENT '国家',
  province VARCHAR(50) COMMENT '省份',
  city VARCHAR(50) COMMENT '城市',
  latitude DECIMAL(10, 7) COMMENT '纬度',
  longitude DECIMAL(10, 7) COMMENT '经度',
  login_count INT DEFAULT 1 COMMENT '登录次数',
  first_login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '首次登录时间',
  last_login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后登录时间',
  FOREIGN KEY (user_id) REFERENCES user(id),
  UNIQUE KEY uk_user_location (user_id, city, latitude, longitude),
  INDEX idx_user_id (user_id),
  INDEX idx_last_login_time (last_login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户常用登录地表';

