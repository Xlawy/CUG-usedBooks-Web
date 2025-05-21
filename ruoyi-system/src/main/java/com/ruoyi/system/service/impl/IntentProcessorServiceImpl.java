package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.SysConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.IIntentProcessorService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 意图处理服务实现
 *
 * @author ruoyi
 */
@Service
public class IntentProcessorServiceImpl implements IIntentProcessorService
{
    private static final Logger log = LoggerFactory.getLogger(IntentProcessorServiceImpl.class);

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisCache redisCache;

    // 微信云函数配置
    @Value("${wx.cloud.appid:wxe26635509bb0f6f3}")
    private String wxAppId;

    @Value("${wx.cloud.env:cloud1-0gjr8zke3634e2e7}")
    private String wxCloudEnv;

    // 微信云函数API地址
    private static final String WX_CLOUD_FUNCTION_URL = "https://api.weixin.qq.com/tcb/invokecloudfunction";

    /**
     * 处理意图
     */
    @Override
    public Map<String, Object> processIntent(String intent, Map<String, Object> parameters)
    {
        log.info("处理意图: intent={}, parameters={}", intent, parameters);

        Map<String, Object> result = new HashMap<>();
        result.put("intent", intent);
        result.put("parameters", parameters);

        try {
            // 根据意图类型分发到不同的处理方法
            Map<String, Object> intentResult;
            switch (intent) {
                case "book_search":
                    intentResult = processBookSearch(parameters);
                    break;
                case "book_info":
                    intentResult = processBookInfo(parameters);
                    break;
                case "order_status":
                    intentResult = processOrderStatus(parameters);
                    break;
                case "book_recommendation":
                    intentResult = processBookRecommendation(parameters);
                    break;
                case "unknown":
                default:
                    intentResult = new HashMap<>();
                    intentResult.put("success", false);
                    intentResult.put("message", "未能识别或不支持的意图类型");
                    break;
            }

            result.put("result", intentResult);
            result.put("success", intentResult.containsKey("success") ? intentResult.get("success") : true);

        } catch (Exception e) {
            log.error("处理意图失败: " + e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 处理图书搜索意图
     */
    @Override
    public Map<String, Object> processBookSearch(Map<String, Object> parameters)
    {
        String bookName = (String) parameters.get("bookName");
        String author = (String) parameters.get("author");
        String isbn = (String) parameters.get("isbn");
        String category = (String) parameters.get("category");

        // 调用微信云函数查询图书 - 使用getAllBooks而不是searchBooks
        Map<String, Object> cloudFunctionResult = invokeCloudFunction("getAllBooks", new HashMap<>());

        Map<String, Object> result = new HashMap<>();

        if (cloudFunctionResult != null && cloudFunctionResult.containsKey("success") && (Boolean) cloudFunctionResult.get("success")) {
            List<Map<String, Object>> allBooks = (List<Map<String, Object>>) cloudFunctionResult.get("data");
            
            // 在本地过滤结果
            List<Map<String, Object>> filteredBooks = new ArrayList<>();
            if (allBooks != null && !allBooks.isEmpty()) {
                for (Map<String, Object> book : allBooks) {
                    boolean matches = true;
                    
                    if (StringUtils.isNotEmpty(bookName)) {
                        String title = (String) book.get("title");
                        if (title == null || !title.toLowerCase().contains(bookName.toLowerCase())) {
                            matches = false;
                        }
                    }
                    
                    if (matches && StringUtils.isNotEmpty(author)) {
                        String bookAuthor = (String) book.get("author");
                        if (bookAuthor == null || !bookAuthor.toLowerCase().contains(author.toLowerCase())) {
                            matches = false;
                        }
                    }
                    
                    if (matches && StringUtils.isNotEmpty(isbn)) {
                        String bookIsbn = (String) book.get("isbn");
                        if (bookIsbn == null || !bookIsbn.equals(isbn)) {
                            matches = false;
                        }
                    }
                    
                    if (matches && StringUtils.isNotEmpty(category)) {
                        String bookCategory = (String) book.get("category");
                        if (bookCategory == null || !bookCategory.toLowerCase().contains(category.toLowerCase())) {
                            matches = false;
                        }
                    }
                    
                    if (matches) {
                        filteredBooks.add(book);
                    }
                }
            }
            
            result.put("success", true);
            result.put("count", filteredBooks.size());
            result.put("data", filteredBooks);
            
            if (filteredBooks.isEmpty()) {
                result.put("message", "未找到符合条件的图书");
            } else {
                result.put("message", String.format("找到%d本符合条件的图书", filteredBooks.size()));
            }
        } else {
            result.put("success", false);
            result.put("message", cloudFunctionResult != null ? cloudFunctionResult.get("message") : "查询图书失败");
        }
        
        return result;
    }

    /**
     * 处理图书详情意图
     */
    @Override
    public Map<String, Object> processBookInfo(Map<String, Object> parameters)
    {
        String bookName = (String) parameters.get("bookName");
        String field = (String) parameters.get("field");

        Map<String, Object> result = new HashMap<>();

        if (StringUtils.isEmpty(bookName)) {
            result.put("success", false);
            result.put("message", "未提供图书名称");
            return result;
        }

        // 调用微信云函数查询图书详情 - 使用getAllBooks而不是getBookDetail
        Map<String, Object> cloudFunctionResult = invokeCloudFunction("getAllBooks", new HashMap<>());

        if (cloudFunctionResult != null && cloudFunctionResult.containsKey("success") && (Boolean) cloudFunctionResult.get("success")) {
            // 从所有图书中找到匹配的图书
            List<Map<String, Object>> allBooks = (List<Map<String, Object>>) cloudFunctionResult.get("data");
            Map<String, Object> bookInfo = null;
            
            if (allBooks != null && !allBooks.isEmpty()) {
                for (Map<String, Object> book : allBooks) {
                    String title = (String) book.get("title");
                    if (title != null && title.toLowerCase().contains(bookName.toLowerCase())) {
                        bookInfo = book;
                        break;
                    }
                }
            }

            if (bookInfo == null || bookInfo.isEmpty()) {
                result.put("success", false);
                result.put("message", String.format("未找到书名包含'%s'的图书", bookName));
                return result;
            }

            // 如果指定了特定字段，只返回该字段的信息
            if (StringUtils.isNotEmpty(field)) {
                Object fieldValue = null;
                String fieldDesc = "";

                switch (field) {
                    case "price":
                        fieldValue = bookInfo.get("price");
                        fieldDesc = "价格";
                        break;
                    case "stock":
                        fieldValue = bookInfo.get("stock");
                        fieldDesc = "库存";
                        break;
                    case "publisher":
                        fieldValue = bookInfo.get("publisher");
                        fieldDesc = "出版社";
                        break;
                    case "isbn":
                        fieldValue = bookInfo.get("isbn");
                        fieldDesc = "ISBN";
                        break;
                    default:
                        fieldValue = bookInfo.get(field);
                        fieldDesc = field;
                        break;
                }

                result.put("success", true);
                result.put("bookName", bookInfo.get("title"));
                result.put("field", field);
                result.put("fieldValue", fieldValue);
                result.put("message", String.format("《%s》的%s是: %s", bookInfo.get("title"), fieldDesc, fieldValue));
            } else {
                // 返回完整的图书信息
                result.put("success", true);
                result.put("data", bookInfo);
                result.put("message", String.format("获取到《%s》的详细信息", bookInfo.get("title")));
            }
        } else {
            result.put("success", false);
            result.put("message", cloudFunctionResult != null ? cloudFunctionResult.get("message") : "查询图书详情失败");
        }

        return result;
    }

    /**
     * 处理订单状态查询意图
     */
    @Override
    public Map<String, Object> processOrderStatus(Map<String, Object> parameters)
    {
        String orderId = (String) parameters.get("orderId");
        String userId = (String) parameters.get("userId");

        Map<String, Object> result = new HashMap<>();

        if (StringUtils.isEmpty(orderId) && StringUtils.isEmpty(userId)) {
            result.put("success", false);
            result.put("message", "未提供订单号或用户ID");
            return result;
        }

        // 调用微信云函数查询订单 - 使用getAllOrders而不是getOrderStatus
        Map<String, Object> cloudFunctionResult = invokeCloudFunction("getAllOrders", new HashMap<>());

        if (cloudFunctionResult != null && cloudFunctionResult.containsKey("success") && (Boolean) cloudFunctionResult.get("success")) {
            List<Map<String, Object>> allOrders = (List<Map<String, Object>>) cloudFunctionResult.get("data");
            
            if (allOrders == null || allOrders.isEmpty()) {
                result.put("success", false);
                result.put("message", "未找到任何订单记录");
                return result;
            }
            
            if (StringUtils.isNotEmpty(orderId)) {
                // 按订单ID查询
                Map<String, Object> orderInfo = null;
                for (Map<String, Object> order : allOrders) {
                    String id = String.valueOf(order.get("orderId"));
                    if (id != null && id.equals(orderId)) {
                        orderInfo = order;
                        break;
                    }
                }
                
                if (orderInfo == null) {
                    result.put("success", false);
                    result.put("message", String.format("未找到订单号为'%s'的订单", orderId));
                    return result;
                }

                result.put("success", true);
                result.put("data", orderInfo);
                result.put("message", String.format("订单%s的状态是: %s", orderId, orderInfo.get("status")));
            } else {
                // 按用户ID查询
                List<Map<String, Object>> userOrders = new ArrayList<>();
                for (Map<String, Object> order : allOrders) {
                    String uid = String.valueOf(order.get("userId"));
                    if (uid != null && uid.equals(userId)) {
                        userOrders.add(order);
                    }
                }

                if (userOrders.isEmpty()) {
                    result.put("success", false);
                    result.put("message", String.format("未找到用户'%s'的订单", userId));
                    return result;
                }

                result.put("success", true);
                result.put("data", userOrders);
                result.put("count", userOrders.size());
                result.put("message", String.format("用户%s有%d个订单", userId, userOrders.size()));
            }
        } else {
            result.put("success", false);
            result.put("message", cloudFunctionResult != null ? cloudFunctionResult.get("message") : "查询订单状态失败");
        }

        return result;
    }

    /**
     * 处理图书推荐意图
     */
    @Override
    public Map<String, Object> processBookRecommendation(Map<String, Object> parameters)
    {
        String category = (String) parameters.get("category");

        Map<String, Object> result = new HashMap<>();

        // 调用微信云函数获取推荐图书 - 使用getAllBooks而不是getRecommendedBooks
        Map<String, Object> cloudFunctionResult = invokeCloudFunction("getAllBooks", new HashMap<>());

        if (cloudFunctionResult != null && cloudFunctionResult.containsKey("success") && (Boolean) cloudFunctionResult.get("success")) {
            List<Map<String, Object>> allBooks = (List<Map<String, Object>>) cloudFunctionResult.get("data");
            List<Map<String, Object>> recommendedBooks = new ArrayList<>();
            
            // 根据分类过滤图书
            if (allBooks != null && !allBooks.isEmpty()) {
                // 首先按分类过滤
                if (StringUtils.isNotEmpty(category)) {
                    for (Map<String, Object> book : allBooks) {
                        String bookCategory = (String) book.get("category");
                        if (bookCategory != null && bookCategory.toLowerCase().contains(category.toLowerCase())) {
                            recommendedBooks.add(book);
                        }
                    }
                } else {
                    // 如果没有指定分类，复制所有图书
                    recommendedBooks.addAll(allBooks);
                }
                
                // 如果结果较多，随机选择最多3本
                if (recommendedBooks.size() > 3) {
                    List<Map<String, Object>> selectedBooks = new ArrayList<>();
                    // 使用简单随机选择
                    java.util.Random random = new java.util.Random();
                    for (int i = 0; i < 3; i++) {
                        int randomIndex = random.nextInt(recommendedBooks.size());
                        selectedBooks.add(recommendedBooks.get(randomIndex));
                        recommendedBooks.remove(randomIndex); // 避免重复选择
                    }
                    recommendedBooks = selectedBooks;
                }
            }

            result.put("success", true);
            result.put("count", recommendedBooks.size());
            result.put("data", recommendedBooks);

            if (recommendedBooks.isEmpty()) {
                if (StringUtils.isNotEmpty(category)) {
                    result.put("message", String.format("没有找到%s类别的图书推荐", category));
                } else {
                    result.put("message", "没有找到推荐图书");
                }
            } else {
                if (StringUtils.isNotEmpty(category)) {
                    result.put("message", String.format("为您推荐%d本%s类别的图书", recommendedBooks.size(), category));
                } else {
                    result.put("message", "为您推荐以下图书");
                }
            }
        } else {
            result.put("success", false);
            result.put("message", cloudFunctionResult != null ? cloudFunctionResult.get("message") : "获取图书推荐失败");
        }

        return result;
    }

    /**
     * 调用微信云函数
     *
     * @param functionName 云函数名称
     * @param params 参数
     * @return 云函数返回结果
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeCloudFunction(String functionName, Map<String, Object> params) {
        try {
            // 获取微信小程序的access_token
            String accessToken = getWxAccessToken();

            if (StringUtils.isEmpty(accessToken)) {
                log.error("无法获取微信小程序access_token");
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "无法获取微信接口访问凭证");
                return errorResult;
            }

            // 构建请求URL
            String url = String.format("%s?access_token=%s&env=%s&name=%s",
                    WX_CLOUD_FUNCTION_URL, accessToken, wxCloudEnv, functionName);

            // 构建请求体
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonParams = JSON.toJSONString(params);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonParams, headers);

            // 发送请求
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                String responseBody = responseEntity.getBody();
                JSONObject responseJson = JSON.parseObject(responseBody);

                if (responseJson.getIntValue("errcode") == 0) {
                    String respData = responseJson.getString("resp_data");
                    return JSON.parseObject(respData, Map.class);
                } else {
                    log.error("微信云函数调用失败: {}", responseJson.getString("errmsg"));
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("message", "调用云函数失败: " + responseJson.getString("errmsg"));
                    return errorResult;
                }
            } else {
                log.error("微信云函数调用HTTP请求失败: {}", responseEntity.getStatusCode());
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "调用云函数HTTP请求失败: " + responseEntity.getStatusCode());
                return errorResult;
            }
        } catch (Exception e) {
            log.error("调用微信云函数异常: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "调用云函数异常: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 获取微信小程序access_token
     *
     * @return access_token
     */
    private String getWxAccessToken() {
        try {
            // 从系统配置中获取
            String cachedToken = configService.selectConfigByKey("wx.access_token");
            String expiresAt = configService.selectConfigByKey("wx.access_token.expires_at");
            
            // 检查token是否有效
            if (StringUtils.isNotEmpty(cachedToken) && StringUtils.isNotEmpty(expiresAt)) {
                long expireTime = Long.parseLong(expiresAt);
                if (System.currentTimeMillis() < expireTime) {
                    return cachedToken;
                }
            }
            
            // 获取微信小程序配置
            String appId = configService.selectConfigByKey("wx.appid");
            String appSecret = configService.selectConfigByKey("wx.appsecret");
            
            if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(appSecret)) {
                log.error("微信小程序配置缺失");
                return null;
            }
            
            // 调用微信API获取access_token
            String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                appId, appSecret);
            
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
            
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonResponse = JSON.parseObject(responseEntity.getBody());
                
                if (jsonResponse.containsKey("access_token")) {
                    String accessToken = jsonResponse.getString("access_token");
                    int expiresIn = jsonResponse.getIntValue("expires_in");
                    
                    // 保存token到Redis缓存，无需使用数据库更新
                    // 直接使用RedisCache服务设置值
                    try {
                        // 更新缓存而不是更新数据库
                        String tokenCacheKey = CacheConstants.SYS_CONFIG_KEY + "wx.access_token";
                        String expiresCacheKey = CacheConstants.SYS_CONFIG_KEY + "wx.access_token.expires_at";
                        long newExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);
                        
                        // 使用配置服务来更新缓存
                        redisCache.setCacheObject(tokenCacheKey, accessToken);
                        redisCache.setCacheObject(expiresCacheKey, String.valueOf(newExpiresAt));
                    } catch (Exception e) {
                        log.warn("无法更新access_token缓存: {}", e.getMessage());
                        // 缓存更新失败仍然可以继续，因为我们已经获取了新token
                    }
                    
                    return accessToken;
                } else {
                    log.error("获取微信access_token失败: {}", jsonResponse.getString("errmsg"));
                    return null;
                }
            } else {
                log.error("获取微信access_token HTTP请求失败: {}", responseEntity.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("获取微信access_token异常: {}", e.getMessage(), e);
            return null;
        }
    }
}
