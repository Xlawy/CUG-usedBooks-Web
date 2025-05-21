package com.ruoyi.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.IAIService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * AI服务接口实现
 *
 * @author ruoyi
 */
@Service
public class AIServiceImpl implements IAIService
{
    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

    private static final String CACHE_KEY_AI_CONFIG = "ai:config";

    private static final String DEFAULT_AI_MODEL = "gpt-3.5-turbo";

    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个校园二手书交易平台的AI助手，你的职责是分析用户自然语言查询意图，并将其转换为结构化JSON格式。\n\n" +
                                                    "支持的查询类型包括：\n" +
                                                    "1. 图书搜索 (book_search) - 查询系统图书库存是否存在某本图书\n" +
                                                    "2. 发布图书搜索 (published_book_search) - 查询是否有用户发布在售的特定图书\n" +
                                                    "3. 图书详情 (book_info) - 获取某本书的库存、价格等详细信息\n" +
                                                    "4. 订单状态 (order_status) - 查询订单状态\n" +
                                                    "5. 图书推荐 (book_recommendation) - 获取图书推荐\n" +
                                                    "6. 信息统计 (statistics) - 获取平台统计信息，如总用户数、总订单量、图书销量等\n" +
                                                    "7. 创建订单 (order_create) - 指导用户创建新的购书订单\n\n" +
                                                    "你必须始终返回以下结构的JSON：\n" +
                                                    "{\n" +
                                                    "  \"intent\": \"意图类型\",  // 必须是以下值之一: book_search, published_book_search, book_info, order_status, book_recommendation, statistics, order_create, unknown\n" +
                                                    "  \"parameters\": {  // 提取自用户问题的参数\n" +
                                                    "    // 可能的参数包括: bookName, author, isbn, category, orderId, userId, timeRange, statType等\n" +
                                                    "  }\n" +
                                                    "}\n\n" +
                                                    "例如，对于\"有没有离散数学这本书\"，应该返回：\n" +
                                                    "{\n" +
                                                    "  \"intent\": \"book_search\",\n" +
                                                    "  \"parameters\": {\n" +
                                                    "    \"bookName\": \"离散数学\"\n" +
                                                    "  }\n" +
                                                    "}\n\n" +
                                                    "对于\"有用户在出售Java编程思想这本书吗\"，应该返回：\n" +
                                                    "{\n" +
                                                    "  \"intent\": \"published_book_search\",\n" +
                                                    "  \"parameters\": {\n" +
                                                    "    \"bookName\": \"Java编程思想\"\n" +
                                                    "  }\n" +
                                                    "}\n\n" +
                                                    "对于\"平台总共有多少用户\"，应该返回：\n" +
                                                    "{\n" +
                                                    "  \"intent\": \"statistics\",\n" +
                                                    "  \"parameters\": {\n" +
                                                    "    \"statType\": \"users\"\n" +
                                                    "  }\n" +
                                                    "}\n\n" +
                                                    "注意事项：\n" +
                                                    "- 你只负责提取意图和参数，不要执行实际查询\n" +
                                                    "- 只返回JSON格式数据，不要添加任何其他文字或解释\n" +
                                                    "- 如果无法识别意图，将intent设为unknown并保持parameters为空对象{}\n" +
                                                    "- 你的回复必须是一个有效的JSON\n" +
                                                    "- 统计意图(statistics)的statType参数值只能是以下几种：users(用户统计)、orders(订单统计)、books(图书统计)、publishedBooks(在售图书统计)";

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    /**
     * 获取AI配置
     */
    @Override
    public Map<String, Object> getAIConfig()
    {
        // 优先从Redis缓存获取
        Map<String, Object> config = redisCache.getCacheObject(CACHE_KEY_AI_CONFIG);
        if (config != null)
        {
            return config;
        }

        // 从系统配置中获取
        config = new HashMap<>();

        String apiKey = configService.selectConfigByKey("ai.api.key");
        String apiUrl = configService.selectConfigByKey("ai.api.url");
        String model = configService.selectConfigByKey("ai.model");
        String systemPrompt = configService.selectConfigByKey("ai.system.prompt");
        String temperatureStr = configService.selectConfigByKey("ai.temperature");
        String maxTokensStr = configService.selectConfigByKey("ai.max.tokens");

        config.put("apiKey", StringUtils.isNotEmpty(apiKey) ? apiKey : "");
        config.put("apiUrl", StringUtils.isNotEmpty(apiUrl) ? apiUrl : "https://api.openai.com/v1/chat/completions");
        config.put("model", StringUtils.isNotEmpty(model) ? model : DEFAULT_AI_MODEL);
        config.put("systemPrompt", StringUtils.isNotEmpty(systemPrompt) ? systemPrompt : DEFAULT_SYSTEM_PROMPT);

        try {
            config.put("temperature", StringUtils.isNotEmpty(temperatureStr) ? Double.parseDouble(temperatureStr) : 0.7);
        } catch (Exception e) {
            config.put("temperature", 0.7);
        }

        try {
            config.put("maxTokens", StringUtils.isNotEmpty(maxTokensStr) ? Integer.parseInt(maxTokensStr) : 800);
        } catch (Exception e) {
            config.put("maxTokens", 800);
        }

        // 缓存配置信息
        redisCache.setCacheObject(CACHE_KEY_AI_CONFIG, config);

        return config;
    }

    /**
     * 更新AI配置
     */
    @Override
    public boolean updateAIConfig(Map<String, Object> config)
    {
        try
        {
            // 更新系统配置
            if (config.containsKey("apiKey")) {
                updateConfig("ai.api.key", String.valueOf(config.get("apiKey")));
            }

            if (config.containsKey("apiUrl")) {
                updateConfig("ai.api.url", String.valueOf(config.get("apiUrl")));
            }

            if (config.containsKey("model")) {
                updateConfig("ai.model", String.valueOf(config.get("model")));
            }

            if (config.containsKey("systemPrompt")) {
                updateConfig("ai.system.prompt", String.valueOf(config.get("systemPrompt")));
            }

            if (config.containsKey("temperature")) {
                updateConfig("ai.temperature", String.valueOf(config.get("temperature")));
            }

            if (config.containsKey("maxTokens")) {
                updateConfig("ai.max.tokens", String.valueOf(config.get("maxTokens")));
            }

            // 更新缓存
            Map<String, Object> currentConfig = getAIConfig();
            currentConfig.putAll(config);
            redisCache.setCacheObject(CACHE_KEY_AI_CONFIG, currentConfig);

            return true;
        }
        catch (Exception e)
        {
            log.error("更新AI配置失败", e);
            return false;
        }
    }

    /**
     * 更新配置
     */
    private void updateConfig(String key, String value) {
        SysConfig config = new SysConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        configService.updateConfig(config);
    }

    /**
     * 发送消息到AI服务
     */
    @Override
    public Map<String, Object> sendMessage(Map<String, Object> params)
    {
        String message = (String) params.get("message");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) params.get("history");
        
        // 记录请求日志，但隐藏敏感信息
        if (log.isDebugEnabled()) {
            log.debug("发送消息到AI服务: message={}, historySize={}", 
                      message != null ? message.substring(0, Math.min(50, message.length())) + "..." : "null", 
                      history != null ? history.size() : 0);
        }
        
        // 获取配置
        Map<String, Object> config = getAIConfig();
        String apiKey = (String) config.get("apiKey");
        String apiUrl = (String) config.get("apiUrl");
        String model = (String) config.get("model");
        double temperature = (double) config.get("temperature");
        int maxTokens = (int) config.get("maxTokens");
        
        // 检查API密钥是否配置
        if (StringUtils.isEmpty(apiKey)) {
            log.warn("AI服务API密钥未配置，将使用本地模拟响应");
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("content", getSimulatedResponse(message));
            result.put("model", "本地模拟模型");
            result.put("isSimulated", true);
            return result;
        }
        
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        
        JSONArray messages = new JSONArray();
        
        // 如果提供了历史记录，直接使用
        if (history != null && !history.isEmpty())
        {
            for (Map<String, String> msg : history)
            {
                JSONObject jsonMsg = new JSONObject();
                jsonMsg.put("role", msg.get("role"));
                jsonMsg.put("content", msg.get("content"));
                messages.add(jsonMsg);
            }
        }
        else
        {
            // 添加系统提示
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", config.get("systemPrompt"));
            messages.add(systemMsg);
            
            // 添加用户消息
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);
        }
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        
        try
        {
            // 调用AI服务
            long startTime = System.currentTimeMillis();
            String responseStr = callAIService(apiUrl, apiKey, requestBody.toString());
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            // 记录请求耗时
            log.info("AI服务调用成功，耗时: {}ms", responseTime);
            
            // 解析响应
            JSONObject response = JSON.parseObject(responseStr);
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            String content = choice.getJSONObject("message").getString("content");
            
            // 获取使用的tokens
            Map<String, Object> usageInfo = new HashMap<>();
            if (response.containsKey("usage")) {
                JSONObject usage = response.getJSONObject("usage");
                log.debug("AI服务使用的tokens: 提示tokens={}, 完成tokens={}, 总tokens={}",
                         usage.getIntValue("prompt_tokens", 0),
                         usage.getIntValue("completion_tokens", 0),
                         usage.getIntValue("total_tokens", 0));
                
                usageInfo.put("promptTokens", usage.getIntValue("prompt_tokens", 0));
                usageInfo.put("completionTokens", usage.getIntValue("completion_tokens", 0));
                usageInfo.put("totalTokens", usage.getIntValue("total_tokens", 0));
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("content", content);
            result.put("model", response.getString("model"));
            result.put("responseTime", responseTime);
            result.put("usage", usageInfo);
            
            return result;
        }
        catch (Exception e)
        {
            log.error("调用AI服务失败: " + e.getMessage(), e);
            
            // 如果调用失败，使用本地模拟回复
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("content", getSimulatedResponse(message));
            result.put("model", "本地模拟模型(错误回退)");
            result.put("isSimulated", true); // 标记为模拟响应
            result.put("error", e.getMessage());
            
            return result;
        }
    }

    /**
     * 调用AI服务
     */
    private String callAIService(String apiUrl, String apiKey, String requestBody) throws Exception
    {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        
        // 设置超时时间
        connection.setConnectTimeout(10000); // 连接超时：10秒
        connection.setReadTimeout(30000);    // 读取超时：30秒
        
        // 发送请求
        try (OutputStream os = connection.getOutputStream())
        {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // 检查响应状态
        int statusCode = connection.getResponseCode();
        if (statusCode != 200) {
            // 读取错误响应
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = br.readLine()) != null)
                {
                    errorResponse.append(line);
                }
            }
            
            log.error("AI服务返回错误，状态码：{}，错误信息：{}", statusCode, errorResponse.toString());
            throw new Exception("AI服务调用失败: HTTP " + statusCode + ", " + errorResponse.toString());
        }
        
        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                response.append(line);
            }
        }
        
        return response.toString();
    }

    /**
     * 获取模拟响应（备用方案）
     */
    private String getSimulatedResponse(String message)
    {
        // 根据关键词构建模拟的结构化JSON响应
        JSONObject response = new JSONObject();
        JSONObject parameters = new JSONObject();
        
        if (message.contains("图书") && (message.contains("有") || message.contains("存在") || message.contains("找")))
        {
            response.put("intent", "book_search");
            // 尝试提取书名
            String bookName = extractBookName(message);
            if (bookName != null) {
                parameters.put("bookName", bookName);
            }
            
            // 尝试提取作者
            if (message.contains("作者") && message.indexOf("作者") + 2 < message.length()) {
                String authorPart = message.substring(message.indexOf("作者") + 2).trim();
                if (!authorPart.isEmpty()) {
                    // 简单提取，取第一个标点符号前的内容
                    int endIndex = authorPart.indexOf("，");
                    if (endIndex == -1) endIndex = authorPart.indexOf(",");
                    if (endIndex == -1) endIndex = authorPart.indexOf("的");
                    if (endIndex == -1) endIndex = authorPart.indexOf("？");
                    if (endIndex == -1) endIndex = authorPart.length();
                    
                    parameters.put("author", authorPart.substring(0, endIndex).trim());
                }
            }
        }
        else if (message.contains("价格") || message.contains("库存") || message.contains("出版"))
        {
            response.put("intent", "book_info");
            // 提取书名
            String bookName = extractBookName(message);
            if (bookName != null) {
                parameters.put("bookName", bookName);
            }
            
            // 提取查询字段
            if (message.contains("价格")) {
                parameters.put("field", "price");
            } else if (message.contains("库存")) {
                parameters.put("field", "stock");
            } else if (message.contains("出版")) {
                parameters.put("field", "publisher");
            }
        }
        else if (message.contains("订单") && (message.contains("状态") || message.contains("进度") || message.contains("查询")))
        {
            response.put("intent", "order_status");
            // 提取订单ID - 简单模拟，假设订单ID是以"订单"后面跟着的数字
            if (message.contains("订单号") || message.contains("订单ID")) {
                int startIndex = message.indexOf("订单号");
                if (startIndex == -1) startIndex = message.indexOf("订单ID");
                if (startIndex != -1 && startIndex + 4 < message.length()) {
                    String orderPart = message.substring(startIndex + 4).trim();
                    // 提取数字
                    StringBuilder orderId = new StringBuilder();
                    for (char c : orderPart.toCharArray()) {
                        if (Character.isDigit(c)) {
                            orderId.append(c);
                        } else if (orderId.length() > 0) {
                            break;
                        }
                    }
                    if (orderId.length() > 0) {
                        parameters.put("orderId", orderId.toString());
                    }
                }
            }
        }
        else if (message.contains("推荐") && message.contains("书"))
        {
            response.put("intent", "book_recommendation");
            // 提取可能的分类或关键词
            if (message.contains("类别") || message.contains("分类")) {
                String category = "";
                if (message.contains("小说")) category = "小说";
                else if (message.contains("历史")) category = "历史";
                else if (message.contains("科技")) category = "科技";
                else if (message.contains("教材")) category = "教材";
                
                if (!category.isEmpty()) {
                    parameters.put("category", category);
                }
            }
        }
        else
        {
            // 未知意图
            response.put("intent", "unknown");
        }
        
        response.put("parameters", parameters);
        return response.toJSONString();
    }
    
    /**
     * 从消息中提取书名 - 简单实现
     */
    private String extractBookName(String message) {
        // 查找常见的书名引导词
        String[] bookIndicators = {"书名", "书籍", "图书", "《"};
        
        for (String indicator : bookIndicators) {
            int index = message.indexOf(indicator);
            if (index >= 0 && index + indicator.length() < message.length()) {
                if (indicator.equals("《")) {
                    // 如果是书名号，查找闭合的书名号
                    int endIndex = message.indexOf("》", index);
                    if (endIndex > index) {
                        return message.substring(index + 1, endIndex);
                    }
                } else {
                    // 其他引导词，取后面的一段文字作为书名
                    String rest = message.substring(index + indicator.length()).trim();
                    // 找到第一个标点或空格作为结束
                    int endIndex = Math.min(
                        firstIndexOfAny(rest, new char[] {'，', ',', '？', '?', '。', '.', ' '}),
                        rest.length()
                    );
                    return rest.substring(0, endIndex).trim();
                }
            }
        }
        return null;
    }
    
    /**
     * 查找字符串中第一个出现的指定字符数组中任何一个字符的位置
     */
    private int firstIndexOfAny(String str, char[] chars) {
        int minIndex = str.length();
        for (char c : chars) {
            int index = str.indexOf(c);
            if (index >= 0 && index < minIndex) {
                minIndex = index;
            }
        }
        return minIndex;
    }
}
