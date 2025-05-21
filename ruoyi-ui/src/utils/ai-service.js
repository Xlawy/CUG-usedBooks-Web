/**
 * AI服务工具类
 * 提供AI聊天相关的工具函数
 */

// AI服务配置
const DEFAULT_CONFIG = {
  // 系统角色提示词
  systemPrompt: "你是一个校园二手书交易平台的AI助手，你的职责是分析用户自然语言查询意图，并将其转换为结构化JSON格式。\n\n" +
                "支持的查询类型包括：\n" +
                "1. 图书搜索 (book_search) - 查询图书是否存在\n" +
                "2. 图书详情 (book_info) - 获取某本书的库存、价格等信息\n" +
                "3. 订单状态 (order_status) - 查询订单状态\n" +
                "4. 图书推荐 (book_recommendation) - 获取图书推荐\n\n" +
                "你必须始终返回以下结构的JSON：\n" +
                "{\n" +
                "  \"intent\": \"意图类型\",  // 必须是以下值之一: book_search, book_info, order_status, book_recommendation, unknown\n" +
                "  \"parameters\": {  // 提取自用户问题的参数\n" +
                "    // 可能的参数包括: bookName, author, isbn, category, orderId, userId等\n" +
                "  }\n" +
                "}\n\n" +
                "例如，对于\"有没有离散数学这本书\"，应该返回：\n" +
                "{\n" +
                "  \"intent\": \"book_search\",\n" +
                "  \"parameters\": {\n" +
                "    \"bookName\": \"离散数学\"\n" +
                "  }\n" +
                "}\n\n" +
                "请记住：\n" +
                "- 你只负责提取意图和参数，不要执行实际查询\n" +
                "- 只返回JSON格式数据，不要添加任何其他文字或解释\n" +
                "- 如果无法识别意图，将intent设为unknown并保持parameters为空对象{}\n" +
                "- 你的回复必须是一个有效的JSON",
  
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
  console.log("生成模拟响应，用户消息:", message);
  
  // 构建模拟的结构化JSON响应
  const response = {
    intent: "unknown",
    parameters: {}
  };
  
  // 清理消息文本，便于关键词匹配
  const cleanMessage = message.toLowerCase();
  
  // 根据关键词提取意图和参数
  if (cleanMessage.includes("图书") || cleanMessage.includes("书") || 
      cleanMessage.includes("有没有") || cleanMessage.includes("找") || 
      cleanMessage.includes("查询") || cleanMessage.includes("搜索")) {
    
    response.intent = "book_search";
    
    // 尝试提取书名 - 方法1: 引号括起来的部分
    if (message.includes("《") && message.includes("》")) {
      const bookName = message.substring(message.indexOf("《") + 1, message.indexOf("》"));
      if (bookName) {
        response.parameters.bookName = bookName;
      }
    }
    // 方法2: 书名关键词后面的部分
    else if (cleanMessage.includes("书名") || cleanMessage.includes("叫") || cleanMessage.includes("名字")) {
      const keywords = ["书名", "叫", "名字", "名称"];
      for (const keyword of keywords) {
        if (cleanMessage.includes(keyword)) {
          const parts = message.split(keyword);
          if (parts.length > 1 && parts[1].trim()) {
            // 提取后面的内容，直到遇到标点符号
            const extracted = parts[1].trim().split(/[，。？!,\.:;]/)[0].trim();
            if (extracted) {
              response.parameters.bookName = extracted;
              break;
            }
          }
        }
      }
    }
    // 方法3: 简单提取明显的名词
    else {
      // 从消息中去掉常见的问句，找出可能的书名
      const cleanedText = message
        .replace(/有没有|查一下|帮我找|我想要|有关于|搜索|查询|请问|书籍|图书/g, '')
        .trim();
        
      if (cleanedText && cleanedText.length < 20) { // 假设书名不会太长
        response.parameters.bookName = cleanedText;
      }
    }
    
    // 尝试提取作者
    if (cleanMessage.includes("作者")) {
      const authorPart = message.substring(message.indexOf("作者") + 2).trim();
      if (authorPart) {
        const author = authorPart.split(/[，。？!,\.:;]/)[0].trim();
        if (author) {
          response.parameters.author = author;
        }
      }
    }
  }
  else if (cleanMessage.includes("价格") || cleanMessage.includes("多少钱") || 
           cleanMessage.includes("库存") || cleanMessage.includes("出版社") || 
           cleanMessage.includes("出版")) {
    response.intent = "book_info";
    
    // 提取书名 (与前面类似)
    if (message.includes("《") && message.includes("》")) {
      const bookName = message.substring(message.indexOf("《") + 1, message.indexOf("》"));
      if (bookName) {
        response.parameters.bookName = bookName;
      }
    } else {
      // 简单提取可能的书名
      const commonTerms = ["价格", "多少钱", "库存", "出版社", "出版", "信息", "详情"];
      let possibleBook = message;
      
      for (const term of commonTerms) {
        if (cleanMessage.includes(term)) {
          possibleBook = possibleBook.replace(term, "");
        }
      }
      
      possibleBook = possibleBook.replace(/的|是|有|这本书|这本|请问|查询/g, "").trim();
      
      if (possibleBook && possibleBook.length < 20) {
        response.parameters.bookName = possibleBook;
      }
    }
    
    // 提取查询字段
    if (cleanMessage.includes("价格") || cleanMessage.includes("多少钱")) {
      response.parameters.field = "price";
    } else if (cleanMessage.includes("库存") || cleanMessage.includes("有多少")) {
      response.parameters.field = "stock";
    } else if (cleanMessage.includes("出版社") || cleanMessage.includes("出版")) {
      response.parameters.field = "publisher";
    }
  }
  else if (cleanMessage.includes("订单") && 
          (cleanMessage.includes("状态") || cleanMessage.includes("进度") || 
           cleanMessage.includes("查询") || cleanMessage.includes("情况"))) {
    response.intent = "order_status";
    
    // 尝试提取订单ID - 查找数字序列
    const orderIdMatch = message.match(/\d{4,}/); // 至少4位数字
    if (orderIdMatch) {
      response.parameters.orderId = orderIdMatch[0];
    }
    
    // 尝试提取用户ID
    if (cleanMessage.includes("用户") && !response.parameters.orderId) {
      const userIdMatch = message.match(/用户\s*(\w+)/i);
      if (userIdMatch && userIdMatch[1]) {
        response.parameters.userId = userIdMatch[1];
      }
    }
  }
  else if (cleanMessage.includes("推荐") && 
          (cleanMessage.includes("书") || cleanMessage.includes("图书") || 
           cleanMessage.includes("阅读"))) {
    response.intent = "book_recommendation";
    
    // 尝试提取类别
    const categories = {
      "小说": ["小说", "故事", "文学"],
      "历史": ["历史", "文化", "传记"],
      "科技": ["科技", "科学", "技术"],
      "计算机": ["计算机", "编程", "软件", "it", "开发"],
      "经济": ["经济", "金融", "商业", "管理"],
      "教育": ["教育", "学习", "考试", "教材"]
    };
    
    for (const [category, keywords] of Object.entries(categories)) {
      for (const keyword of keywords) {
        if (cleanMessage.includes(keyword)) {
          response.parameters.category = category;
          break;
        }
      }
      if (response.parameters.category) break;
    }
  }
  
  console.log("生成的模拟响应:", response);
  return JSON.stringify(response, null, 2);
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