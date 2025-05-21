import request from '@/utils/request'

/**
 * 发送消息到AI接口获取回复
 * @param {Object} data 包含用户消息的对象
 * @returns {Promise} 返回AI回复的Promise
 */
export function sendMessageToAI(data) {
  return request({
    url: '/system/ai/chat',
    method: 'post',
    data: data,
    timeout: 60000  // 为AI请求单独设置60秒超时
  })
}

/**
 * 配置备选方案 - 使用第三方AI服务API直接调用
 * @param {Object} data 包含用户消息的对象
 * @returns {Promise} 返回AI回复的Promise
 */
export function sendMessageToThirdPartyAI(data) {
  // 这里使用fetch API直接调用第三方服务
  // 支持OpenAI和ChatAnywhere API
  const apiUrl = process.env.VUE_APP_AI_API_URL || 'https://api.chatanywhere.tech/v1/chat/completions';
  return fetch(apiUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${process.env.VUE_APP_AI_API_KEY || 'your-api-key-here'}`
    },
    body: JSON.stringify({
      model: data.model || "gpt-3.5-turbo",
      messages: data.history || [
        {
          role: "system",
          content: "你是一个校园二手书交易平台的AI助手，可以回答关于图书管理、交易流程、用户账户等问题。"
        },
        {
          role: "user",
          content: data.message
        }
      ],
      temperature: data.temperature || 0.7,
      max_tokens: data.max_tokens || 500
    })
  }).then(response => {
    if (!response.ok) {
      throw new Error(`AI服务响应错误: ${response.status}`);
    }
    return response.json();
  }).then(data => {
    // 根据API返回格式提取回复内容
    if (data && data.choices && data.choices.length > 0) {
      return { 
        success: true, 
        content: data.choices[0].message.content,
        model: data.model,
        usage: data.usage
      };
    } else {
      throw new Error('AI响应格式错误');
    }
  });
}

/**
 * 获取AI接口配置信息
 * @returns {Promise} 返回配置信息的Promise
 */
export function getAIConfig() {
  return request({
    url: '/system/ai/config',
    method: 'get'
  })
}

/**
 * 更新AI接口配置
 * @param {Object} data 配置信息
 * @returns {Promise} 返回操作结果的Promise
 */
export function updateAIConfig(data) {
  return request({
    url: '/system/ai/config',
    method: 'put',
    data: data
  })
} 