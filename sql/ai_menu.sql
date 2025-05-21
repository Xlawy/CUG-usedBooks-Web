-- ----------------------------
-- AI配置相关菜单SQL
-- ----------------------------

-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI服务配置', '1', 10, 'aiconfig', 'system/aiconfig/index', 1, 0, 'C', '0', '0', 'system:ai:list', 'robot', 'admin', sysdate(), '', null, 'AI服务接口参数配置菜单');

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI配置查询', @parentId, 1, '#', '', 1, 0, 'F', '0', '0', 'system:ai:query', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI配置编辑', @parentId, 2, '#', '', 1, 0, 'F', '0', '0', 'system:ai:edit', '#', 'admin', sysdate(), '', null, '');

-- 初始化AI配置参数
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark) 
VALUES 
('AI服务API密钥', 'ai.api.key', 'sk-2iuRZkpuWh5TkbgSegjf8VMF8ez1AwmNkvdAFKpjZ29lUwOA', 'N', 'admin', sysdate(), '', null, 'ChatAnywhere或其他AI服务的API密钥'),
('AI服务API地址', 'ai.api.url', 'https://api.chatanywhere.tech/v1/chat/completions', 'N', 'admin', sysdate(), '', null, 'AI服务的接口地址'),
('AI模型名称', 'ai.model', 'gpt-3.5-turbo', 'N', 'admin', sysdate(), '', null, '使用的AI模型'),
('AI系统提示词', 'ai.system.prompt', '你是一个校园二手书交易平台的AI助手，可以回答关于图书管理、交易流程、用户账户等方面的问题。保持回答简洁专业。', 'N', 'admin', sysdate(), '', null, 'AI角色设定'),
('AI温度参数', 'ai.temperature', '0.7', 'N', 'admin', sysdate(), '', null, '控制AI回答的创造性，0-1之间'),
('AI最大Token数', 'ai.max.tokens', '800', 'N', 'admin', sysdate(), '', null, 'AI回答的最大长度'); 