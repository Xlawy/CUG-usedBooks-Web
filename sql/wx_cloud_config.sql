-- 微信小程序配置插入脚本

-- 插入微信小程序AppID配置
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, remark) 
VALUES ('微信小程序AppID', 'wx.appid', 'wxe26635509bb0f6f3', 'Y', 'admin', '微信小程序的AppID');

-- 插入微信小程序AppSecret配置
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, remark) 
VALUES ('微信小程序AppSecret', 'wx.appsecret', '0fdf6887d03ddc21460112595306e62a', 'Y', 'admin', '微信小程序的AppSecret');

-- 如果需要，可以显式配置微信云函数环境ID（代码中已有默认值）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, remark) 
VALUES ('微信云函数环境ID', 'wx.cloud.env', 'cloud1-0gjr8zke3634e2e7', 'Y', 'admin', '微信云开发环境ID');

-- 添加空的access_token记录，系统将在首次调用时更新
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, remark) 
VALUES ('微信接口访问凭证', 'wx.access_token', '', 'Y', 'admin', '微信接口调用凭证，系统自动获取');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, remark) 
VALUES ('微信凭证过期时间', 'wx.access_token.expires_at', '0', 'Y', 'admin', '微信接口凭证过期时间戳');

-- 注意：请将AppID和AppSecret替换为您实际的微信小程序凭证
-- 如果是测试，可以先使用这些示例值 