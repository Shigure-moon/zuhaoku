-- 日志审计表
-- 用于记录系统关键操作，支持安全审计和问题追踪

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT COMMENT '操作用户ID（可为空，如系统操作）',
  username VARCHAR(50) COMMENT '操作用户名（冗余字段，便于查询）',
  role VARCHAR(20) COMMENT '用户角色：TENANT/OWNER/OPERATOR',
  action VARCHAR(100) NOT NULL COMMENT '操作类型：LOGIN/LOGOUT/CREATE_ORDER/PAYMENT/APPEAL_RESOLVE/USER_FREEZE/USER_UNFREEZE/ACCOUNT_CREATE/ACCOUNT_UPDATE/ACCOUNT_DELETE等',
  resource_type VARCHAR(50) COMMENT '资源类型：USER/ORDER/ACCOUNT/APPEAL/PAYMENT等',
  resource_id BIGINT COMMENT '资源ID',
  description TEXT COMMENT '操作描述',
  request_method VARCHAR(10) COMMENT 'HTTP请求方法：GET/POST/PUT/DELETE等',
  request_path VARCHAR(500) COMMENT '请求路径',
  request_params TEXT COMMENT '请求参数（JSON格式）',
  response_status INT COMMENT '响应状态码',
  ip_address VARCHAR(50) COMMENT '客户端IP地址',
  user_agent VARCHAR(500) COMMENT '用户代理（浏览器信息）',
  success TINYINT(1) DEFAULT 1 COMMENT '操作是否成功：1-成功 0-失败',
  error_message TEXT COMMENT '错误信息（失败时记录）',
  execution_time INT COMMENT '执行耗时（毫秒）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user_id (user_id),
  INDEX idx_action (action),
  INDEX idx_resource (resource_type, resource_id),
  INDEX idx_created_at (created_at),
  INDEX idx_user_action_time (user_id, action, created_at),
  INDEX idx_role_action (role, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志审计表';

