/**
 * AI服务工具类
 * 提供AI聊天相关的工具函数
 */

// AI服务配置
const DEFAULT_CONFIG = {
  // 系统角色提示词
  systemPrompt: "你是一个校园二手书交易平台的AI助手，你的职责是分析用户自然语言查询意图，并将其转换为结构化JSON格式。\n\n" +
                "支持的查询类型包括：\n" +
                "1. 查询图书是否存在（按书名、作者等信息）\n" +
                "2. 获取某本书的库存、价格、出版信息等\n" +
                "3. 查询订单状态\n" +
                "4. 图书推荐\n\n" +
                "你必须始终返回以下结构的JSON：\n" +
                "{\n" +
                "  \"intent\": \"查询意图\",  // book_search(图书搜索), book_info(图书详情), order_status(订单状态), book_recommendation(图书推荐)或unknown(未知意图)\n" +
                "  \"parameters\": {  // 提取自用户问题的参数\n" +
                "    // 可能的参数包括: bookName, author, isbn, category, orderId, userId等\n" +
                "  }\n" +
                "}\n\n" +
                "如果用户的问题不是关于图书查询、订单状态或推荐图书，将intent设为unknown并保持parameters为空对象{}。",
  
  // 温度参数（控制创造性，0-1之间）
  temperature: 0.3, // 降低温度，使输出更可预测
  
  // 最大输出token数量
  maxTokens: 400, // 减小输出长度，因为我们只需要简短的JSON响应
  
  // 默认使用的模型
  model: "gpt-3.5-turbo",
  
  // 是否启用流式响应
  streamResponse: false,
  
  // 每次对话保留的最大历史消息数量
  maxHistoryLength: 5, // 减少历史消息长度，因为每次请求本质上是独立的意图识别
  
  // 超时时间(ms)
  timeout: 30000,
  
  // 重试次数
  retryCount: 2
};

/**
 * 获取API密钥
 * 优先从环境变量获取，如果不存在则使用默认值
 */
export function getApiKey() {
  return process.env.VUE_APP_AI_API_KEY || '';
}

/**
 * 构建发送到AI服务的消息历史
 * @param {Array} messages 当前UI中显示的消息数组
 * @param {String} systemPrompt 系统提示词
 * @param {Number} maxHistory 最大历史长度
 * @returns {Array} 格式化后的消息历史
 */
export function buildChatHistory(messages, systemPrompt = DEFAULT_CONFIG.systemPrompt, maxHistory = DEFAULT_CONFIG.maxHistoryLength) {
  // 构建对话历史记录
  const history = [
    {
      role: "system",
      content: systemPrompt
    }
  ];
  
  // 添加最近的消息记录（最多maxHistory条）
  const recentMessages = messages.slice(-maxHistory);
  recentMessages.forEach(msg => {
    history.push({
      role: msg.isUser ? "user" : "assistant",
      content: msg.content
    });
  });
  
  return history;
}

/**
 * 截断过长的消息
 * @param {String} message 消息文本
 * @param {Number} maxLength 最大长度
 * @returns {String} 截断后的消息
 */
export function truncateMessage(message, maxLength = 2000) {
  if (message.length <= maxLength) {
    return message;
  }
  
  return message.substring(0, maxLength) + "... [消息已截断]";
}

/**
 * 模拟AI响应（本地回退方案）
 * @param {String} message 用户消息
 * @returns {String} 模拟的AI回复 - 现在返回JSON字符串
 */
export function getSimulatedResponse(message) {
  // 构建模拟的结构化JSON响应
  const response = {
    intent: "unknown",
    parameters: {}
  };
  
  // 根据关键词提取意图和参数
  if (message.includes("图书") && (message.includes("有") || message.includes("存在") || message.includes("找"))) {
    response.intent = "book_search";
    // 尝试提取书名
    if (message.includes("《") && message.includes("》")) {
      const bookName = message.substring(message.indexOf("《") + 1, message.indexOf("》"));
      if (bookName) {
        response.parameters.bookName = bookName;
      }
    }
    else if (message.includes("书名")) {
      const parts = message.split("书名");
      if (parts.length > 1 && parts[1].trim()) {
        response.parameters.bookName = parts[1].trim().replace(/[，。？!,\.]/g, '');
      }
    }
  }
  else if (message.includes("价格") || message.includes("库存") || message.includes("出版")) {
    response.intent = "book_info";
    // 提取书名
    if (message.includes("《") && message.includes("》")) {
      const bookName = message.substring(message.indexOf("《") + 1, message.indexOf("》"));
      if (bookName) {
        response.parameters.bookName = bookName;
      }
    }
    
    // 提取查询字段
    if (message.includes("价格")) {
      response.parameters.field = "price";
    } else if (message.includes("库存")) {
      response.parameters.field = "stock";
    } else if (message.includes("出版")) {
      response.parameters.field = "publisher";
    }
  }
  else if (message.includes("订单") && (message.includes("状态") || message.includes("进度"))) {
    response.intent = "order_status";
    // 尝试提取订单ID
    const orderIdMatch = message.match(/\d+/);
    if (orderIdMatch) {
      response.parameters.orderId = orderIdMatch[0];
    }
  }
  else if (message.includes("推荐") && message.includes("书")) {
    response.intent = "book_recommendation";
    // 尝试提取类别
    if (message.includes("小说")) response.parameters.category = "小说";
    else if (message.includes("历史")) response.parameters.category = "历史";
    else if (message.includes("科技")) response.parameters.category = "科技";
    else if (message.includes("计算机")) response.parameters.category = "计算机";
  }
  
  return JSON.stringify(response);
}

/**
 * 格式化AI回复内容
 * @param {String} content AI回复的原始内容
 * @returns {String} 格式化后的内容
 */
export function formatAIResponse(content) {
  if (!content) return '';
  
  // 替换连续的换行为两个换行
  let formatted = content.replace(/\n{3,}/g, '\n\n');
  
  // 替换特殊标记
  formatted = formatted.replace(/```/g, '');
  
  return formatted.trim();
}

export default {
  DEFAULT_CONFIG,
  getApiKey,
  buildChatHistory,
  truncateMessage,
  getSimulatedResponse,
  formatAIResponse
}; 