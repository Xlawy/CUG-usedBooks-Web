# AI聊天功能配置指南

本文档提供了关于如何配置和使用校园二手书管理系统中的AI聊天功能的详细说明。

## 一、功能介绍

AI聊天功能提供了一个悬浮式聊天窗口，允许用户通过自然语言与系统进行交互，获取关于图书管理、交易流程、用户管理等方面的信息和帮助。

主要特性：
- 悬浮式对话框，可自由拖拽
- 支持配置第三方AI服务（如ChatAnywhere、OpenAI等）
- 本地模拟回复作为备选方案
- 支持对话历史记录
- 响应式界面设计，适配移动端

## 二、系统配置

### 1. 数据库配置

执行以下SQL脚本添加必要的菜单和配置项：

```sql
-- 请执行sql/ai_menu.sql文件中的SQL语句
```

### 2. API密钥配置

#### 方式一：通过管理界面配置（推荐）

1. 登录系统管理后台
2. 进入 `系统管理` -> `AI服务配置`
3. 找到 `ai.api.key` 配置项，点击修改
4. 填入您从AI服务提供商处获取的API密钥
5. 点击确定保存

#### 方式二：直接修改数据库

```sql
-- 替换YOUR_API_KEY为您的实际API密钥
UPDATE sys_config SET config_value = 'YOUR_API_KEY' WHERE config_key = 'ai.api.key';
```

### 3. API地址配置

默认使用ChatAnywhere的API地址：`https://api.chatanywhere.tech/v1/chat/completions`

如需使用其他服务，可以修改 `ai.api.url` 配置项：

- OpenAI: `https://api.openai.com/v1/chat/completions`
- 其他API代理服务: 根据服务商提供的地址填写

### 4. ChatAnywhere API说明

ChatAnywhere API与OpenAI API格式兼容，调用格式如下：

```bash
curl https://api.chatanywhere.tech/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_API_KEY' \
  -d '{
  "model": "gpt-3.5-turbo",
  "messages": [{"role": "user", "content": "Say this is a test!"}],
  "temperature": 0.7
}'
```

返回格式：

```json
{
   "id":"chatcmpl-abc123",
   "object":"chat.completion",
   "created":1677858242,
   "model":"gpt-3.5-turbo-0301",
   "usage":{
      "prompt_tokens":13,
      "completion_tokens":7,
      "total_tokens":20
   },
   "choices":[
      {
         "message":{
            "role":"assistant",
            "content":"\n\nThis is a test!"
         },
         "finish_reason":"stop",
         "index":0
      }
   ]
}
```

更多详细信息请参考: [ChatAnywhere API文档](https://chatanywhere.apifox.cn/api-92222076)

### 5. 其他配置项

| 配置键名 | 说明 | 默认值 |
|---------|------|--------|
| ai.model | AI模型名称 | gpt-3.5-turbo |
| ai.system.prompt | 系统提示词 | 你是一个校园二手书交易平台的AI助手... |
| ai.temperature | 温度参数 | 0.7 |
| ai.max.tokens | 最大Token数 | 800 |

## 三、使用说明

### 1. 启用聊天界面

在系统界面右下角会显示一个蓝色的聊天图标，点击即可打开聊天窗口。

### 2. 对话交互

- 在输入框中输入您的问题
- 点击发送按钮或按Enter键发送消息
- AI会根据您的问题给出回复

### 3. 调整窗口位置

您可以通过拖拽聊天窗口顶部的标题栏来调整窗口位置。

## 四、故障排除

### 1. AI无法回复

可能的原因：
- API密钥未配置或配置错误
- 网络连接问题
- AI服务响应超时

解决方案：
- 检查API密钥配置
- 检查网络连接
- 如果问题持续，系统会自动使用本地模拟回复

### 2. 本地模拟回复

当无法连接到AI服务时，系统会自动切换到本地模拟回复模式。这种模式下，回复是基于预设的关键词匹配规则生成的，而非真正的AI生成内容。

## 五、开发信息

### 前端组件

- `AIChat` - 主聊天组件
- `AIChatLauncher` - 聊天启动器组件

### 后端接口

- `/system/ai/chat` - 处理聊天请求
- `/system/ai/config` - 获取和更新AI配置

### 配置文件

- `AIServiceImpl.java` - AI服务实现
- `ai-service.js` - 前端工具类

如需更多技术支持，请联系系统管理员。 