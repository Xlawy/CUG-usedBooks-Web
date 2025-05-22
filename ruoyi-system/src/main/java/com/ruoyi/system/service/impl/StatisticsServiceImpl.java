package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
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

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.IStatisticsService;

/**
 * 统计数据服务实现
 * 
 * @author ruoyi
 */
@Service
public class StatisticsServiceImpl implements IStatisticsService
{
    private static final Logger log = LoggerFactory.getLogger(StatisticsServiceImpl.class);

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

    // 统计数据缓存时间(秒)
    private static final Integer STATISTICS_CACHE_SECONDS = 300;

    // 缓存key前缀
    private static final String CACHE_STATS_PREFIX = "stats:";

    @Override
    public Map<String, Object> getUsersCount() {
        String cacheKey = CACHE_STATS_PREFIX + "users";
        Map<String, Object> cachedResult = redisCache.getCacheObject(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> usersResult = invokeCloudFunction("getAllUsers", new HashMap<>());
        
        if (usersResult != null && usersResult.containsKey("success") && (Boolean) usersResult.get("success")) {
            List<Map<String, Object>> users = (List<Map<String, Object>>) usersResult.get("data");
            int userCount = users != null ? users.size() : 0;
            
            // 从total字段获取总数，如果存在
            if (usersResult.containsKey("total")) {
                userCount = ((Number) usersResult.get("total")).intValue();
            }
            
            result.put("success", true);
            result.put("statType", "users");
            result.put("count", userCount);
            result.put("message", String.format("平台当前共有%d位用户", userCount));
        } else {
            result.put("success", false);
            result.put("message", "获取用户统计信息失败");
        }

        // 缓存结果
        redisCache.setCacheObject(cacheKey, result, STATISTICS_CACHE_SECONDS, TimeUnit.SECONDS);
        return result;
    }

    @Override
    public Map<String, Object> getOrdersCount() {
        String cacheKey = CACHE_STATS_PREFIX + "orders";
        Map<String, Object> cachedResult = redisCache.getCacheObject(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> ordersResult = invokeCloudFunction("getAllOrders", new HashMap<>());
        
        if (ordersResult != null && ordersResult.containsKey("success") && (Boolean) ordersResult.get("success")) {
            List<Map<String, Object>> orders = (List<Map<String, Object>>) ordersResult.get("data");
            int orderCount = orders != null ? orders.size() : 0;
            
            // 从total字段获取总数，如果存在
            if (ordersResult.containsKey("total")) {
                orderCount = ((Number) ordersResult.get("total")).intValue();
            }
            
            result.put("success", true);
            result.put("statType", "orders");
            result.put("count", orderCount);
            result.put("message", String.format("平台当前共有%d个订单", orderCount));
        } else {
            result.put("success", false);
            result.put("message", "获取订单统计信息失败");
        }

        // 缓存结果
        redisCache.setCacheObject(cacheKey, result, STATISTICS_CACHE_SECONDS, TimeUnit.SECONDS);
        return result;
    }

    @Override
    public Map<String, Object> getBooksCount() {
        String cacheKey = CACHE_STATS_PREFIX + "books";
        Map<String, Object> cachedResult = redisCache.getCacheObject(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> booksResult = invokeCloudFunction("getAllBooks", new HashMap<>());
        
        if (booksResult != null && booksResult.containsKey("success") && (Boolean) booksResult.get("success")) {
            List<Map<String, Object>> books = (List<Map<String, Object>>) booksResult.get("data");
            int bookCount = books != null ? books.size() : 0;
            
            // 从total字段获取总数，如果存在
            if (booksResult.containsKey("total")) {
                bookCount = ((Number) booksResult.get("total")).intValue();
            }
            
            result.put("success", true);
            result.put("statType", "books");
            result.put("count", bookCount);
            result.put("message", String.format("平台当前共有%d本图书", bookCount));
        } else {
            result.put("success", false);
            result.put("message", "获取图书统计信息失败");
        }

        // 缓存结果
        redisCache.setCacheObject(cacheKey, result, STATISTICS_CACHE_SECONDS, TimeUnit.SECONDS);
        return result;
    }

    @Override
    public Map<String, Object> getPublishedBooksCount() {
        String cacheKey = CACHE_STATS_PREFIX + "publishedBooks";
        Map<String, Object> cachedResult = redisCache.getCacheObject(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        Map<String, Object> result = new HashMap<>();
        // 构建查询参数，查询状态为在售的图书
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("status", 0);
        
        Map<String, Object> publishedBooksResult = invokeCloudFunction("getAllPublishedBooks", queryParams);
        
        if (publishedBooksResult != null && publishedBooksResult.containsKey("success") && (Boolean) publishedBooksResult.get("success")) {
            List<Map<String, Object>> publishedBooks = (List<Map<String, Object>>) publishedBooksResult.get("data");
            int publishedBookCount = publishedBooks != null ? publishedBooks.size() : 0;
            
            // 从total字段获取总数，如果存在
            if (publishedBooksResult.containsKey("total")) {
                publishedBookCount = ((Number) publishedBooksResult.get("total")).intValue();
            }
            
            result.put("success", true);
            result.put("statType", "publishedBooks");
            result.put("count", publishedBookCount);
            result.put("message", String.format("平台当前共有%d本在售图书", publishedBookCount));
        } else {
            result.put("success", false);
            result.put("message", "获取在售图书统计信息失败");
        }

        // 缓存结果
        redisCache.setCacheObject(cacheKey, result, STATISTICS_CACHE_SECONDS, TimeUnit.SECONDS);
        return result;
    }

    @Override
    public Map<String, Object> getAllStatistics() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> statistics = new HashMap<>();
        
        log.info("开始获取所有统计数据");
        
        try {
            // 并行获取所有统计数据
            Map<String, Object> usersResult = getUsersCount();
            log.info("用户统计数据: {}", usersResult);
            
            Map<String, Object> ordersResult = getOrdersCount();
            log.info("订单统计数据: {}", ordersResult);
            
            Map<String, Object> booksResult = getBooksCount();
            log.info("图书统计数据: {}", booksResult);
            
            Map<String, Object> publishedBooksResult = getPublishedBooksCount();
            log.info("在售图书统计数据: {}", publishedBooksResult);
            
            // 检查是否所有请求都成功
            boolean allSuccess = (Boolean) usersResult.getOrDefault("success", false) &&
                                (Boolean) ordersResult.getOrDefault("success", false) &&
                                (Boolean) booksResult.getOrDefault("success", false) &&
                                (Boolean) publishedBooksResult.getOrDefault("success", false);
            
            // 填充统计数据
            statistics.put("users", usersResult.getOrDefault("count", 0));
            statistics.put("orders", ordersResult.getOrDefault("count", 0));
            statistics.put("books", booksResult.getOrDefault("count", 0));
            statistics.put("publishedBooks", publishedBooksResult.getOrDefault("count", 0));
            
            result.put("success", allSuccess);
            result.put("statistics", statistics);
            
            if (!allSuccess) {
                log.error("部分统计数据获取失败：users={}, orders={}, books={}, publishedBooks={}",
                    usersResult.get("success"),
                    ordersResult.get("success"),
                    booksResult.get("success"),
                    publishedBooksResult.get("success"));
            }
            
        } catch (Exception e) {
            log.error("获取统计数据时发生异常", e);
            result.put("success", false);
            result.put("message", "获取统计数据时发生异常：" + e.getMessage());
        }
        
        log.info("统计数据获取完成: {}", result);
        return result;
    }

    /**
     * 获取指定月份的订单统计数据
     * @param yearMonth 年月，格式：yyyy-MM
     * @return 统计数据
     */
    public Map<String, Object> getMonthlyOrdersStats(String yearMonth) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        
        try {
            // 解析年月
            String[] parts = yearMonth.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            
            // 计算月初和月末时间戳
            Calendar cal = Calendar.getInstance();
            cal.set(year, month - 1, 1, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long monthStart = cal.getTimeInMillis();
            
            cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
            cal.set(Calendar.MILLISECOND, 999);
            long monthEnd = cal.getTimeInMillis();
            
            // 设置查询参数
            params.put("startTime", monthStart);
            params.put("endTime", monthEnd);
            
            Map<String, Object> ordersResult = invokeCloudFunction("getAllOrders", params);
            
            if (ordersResult != null && ordersResult.containsKey("success") && (Boolean) ordersResult.get("success")) {
                List<Map<String, Object>> orders = (List<Map<String, Object>>) ordersResult.get("data");
                
                // 初始化每天的统计数据
                int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                int[] orderCounts = new int[daysInMonth];
                int[] completedOrderCounts = new int[daysInMonth];
                
                // 统计每天的订单数和完成数
                if (orders != null) {
                    for (Map<String, Object> order : orders) {
                        // 获取订单创建时间
                        Long createTime = ((Number) order.get("creat")).longValue();
                        Calendar orderCal = Calendar.getInstance();
                        orderCal.setTimeInMillis(createTime);
                        int day = orderCal.get(Calendar.DAY_OF_MONTH) - 1;
                        
                        // 统计订单数
                        orderCounts[day]++;
                        
                        // 统计已完成订单数（status=2表示已完成）
                        Integer status = ((Number) order.get("status")).intValue();
                        if (status == 2) {
                            completedOrderCounts[day]++;
                        }
                    }
                }
                
                result.put("success", true);
                result.put("expectedData", orderCounts);
                result.put("actualData", completedOrderCounts);
                result.put("message", "获取月度订单统计成功");
            } else {
                result.put("success", false);
                result.put("message", "获取月度订单统计失败");
            }
        } catch (Exception e) {
            log.error("获取月度订单统计异常", e);
            result.put("success", false);
            result.put("message", "获取月度订单统计异常：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 调用微信云函数
     * 
     * @param functionName 云函数名称
     * @param params 函数参数
     * @return 调用结果
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeCloudFunction(String functionName, Map<String, Object> params) {
        try {
            // 检查云函数名称是否在可用的云函数列表中
            List<String> availableFunctions = List.of(
                "addBook", "addOrder", "addPublishedBook", "addUser",
                "deleteBook", "deleteHistoryRecord", "deleteOrder", "deletePublishedBook", "deleteUser",
                "getAllBooks", "getAllOrders", "getAllPublishedBooks", "getAllUsers",
                "updateBook", "updateOrder", "updatePublishedBook", "updateUser",
                "uploadBookCover"
            );

            // 如果请求的函数不在可用列表中，返回错误
            if (!availableFunctions.contains(functionName)) {
                log.error("请求的云函数'{}不存在，使用默认函数", functionName);
                // 根据意图类型使用合适的默认函数
                switch (functionName) {
                    case "searchBooks":
                    case "getBookDetail":
                    case "getRecommendedBooks":
                        functionName = "getAllBooks";
                        break;
                    case "getOrderStatus":
                        functionName = "getAllOrders";
                        break;
                    default:
                        // 默认返回错误
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("message", "请求的云函数'" + functionName + "'不存在");
                        return errorResult;
                }
                log.info("已将请求重定向到云函数: {}", functionName);
            }

            // 获取微信小程序的access_token
            String accessToken = getWxAccessToken(false);
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
            log.info("调用云函数 {}, 参数: {}", functionName, jsonParams);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonParams, headers);

            try {
                // 发送请求
                ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
                log.info("云函数 {} 响应状态: {}", functionName, responseEntity.getStatusCode());

                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    String responseBody = responseEntity.getBody();
                    JSONObject responseJson = JSON.parseObject(responseBody);
                    log.info("云函数 {} 响应内容: {}", functionName, responseBody);

                    if (responseJson.getIntValue("errcode") == 0) {
                        String respData = responseJson.getString("resp_data");
                        Map<String, Object> result = JSON.parseObject(respData, Map.class);
                        if (result == null) {
                            result = new HashMap<>();
                            result.put("success", true);
                            result.put("data", new ArrayList<>());
                            result.put("total", 0);
                        }
                        return result;
                    } else if (responseJson.getIntValue("errcode") == 40001) {
                        // access_token 无效，尝试强制刷新
                        log.info("access_token无效，尝试强制刷新");
                        accessToken = getWxAccessToken(true);
                        if (StringUtils.isNotEmpty(accessToken)) {
                            // 使用新的token重试
                            url = String.format("%s?access_token=%s&env=%s&name=%s",
                                    WX_CLOUD_FUNCTION_URL, accessToken, wxCloudEnv, functionName);
                            responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
                            responseBody = responseEntity.getBody();
                            responseJson = JSON.parseObject(responseBody);
                            log.info("使用新token重试，响应: {}", responseBody);
                            
                            if (responseJson.getIntValue("errcode") == 0) {
                                String respData = responseJson.getString("resp_data");
                                Map<String, Object> result = JSON.parseObject(respData, Map.class);
                                if (result == null) {
                                    result = new HashMap<>();
                                    result.put("success", true);
                                    result.put("data", new ArrayList<>());
                                    result.put("total", 0);
                                }
                                return result;
                            }
                        }
                        log.error("微信云函数调用失败，即使刷新token后也失败: {}", responseJson.getString("errmsg"));
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("message", "调用云函数失败: " + responseJson.getString("errmsg"));
                        return errorResult;
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
                log.error("调用微信云函数HTTP请求异常: {}", e.getMessage(), e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "调用云函数HTTP请求异常: " + e.getMessage());
                return errorResult;
            }
        } catch (Exception e) {
            log.error("调用微信云函数处理异常: {}", e.getMessage(), e);
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
    private String getWxAccessToken(boolean forceRefresh) {
        // 尝试从缓存获取token
        String tokenKey = "wx:access_token:" + wxAppId;
        String accessToken = null;
        
        if (!forceRefresh) {
            accessToken = redisCache.getCacheObject(tokenKey);
            if (StringUtils.isNotEmpty(accessToken)) {
                log.info("从缓存获取access_token成功");
                return accessToken;
            }
        } else {
            log.info("强制刷新access_token");
        }
        
        try {
            // 从微信服务器获取新的access_token
            String appSecret = configService.selectConfigByKey("wx.appsecret");
            log.info("获取到的AppSecret配置: {}", appSecret != null ? "非空" : "为空");
            
            if (StringUtils.isEmpty(appSecret)) {
                log.error("未配置微信小程序secret，请在系统配置中设置wx.appsecret");
                return null;
            }
            
            // 使用新的稳定版接口
            String url = "https://api.weixin.qq.com/cgi-bin/stable_token";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("grant_type", "client_credential");
            requestBody.put("appid", wxAppId);
            requestBody.put("secret", appSecret);
            requestBody.put("force_refresh", forceRefresh);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            log.info("开始获取微信access_token，appId: {}, 请求URL: {}, force_refresh: {}", wxAppId, url, forceRefresh);
            log.info("请求体: {}", JSON.toJSONString(requestBody));
            
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
            
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                String responseBody = responseEntity.getBody();
                log.info("获取access_token响应: {}", responseBody);
                JSONObject responseJson = JSON.parseObject(responseBody);
                
                if (responseJson.containsKey("access_token")) {
                    accessToken = responseJson.getString("access_token");
                    int expiresIn = responseJson.getIntValue("expires_in");
                    
                    // 存入缓存，提前5分钟过期
                    redisCache.setCacheObject(tokenKey, accessToken, expiresIn - 300, TimeUnit.SECONDS);
                    log.info("成功获取新的access_token，有效期: {}秒", expiresIn);
                    
                    return accessToken;
                } else {
                    log.error("获取微信access_token失败: {}", responseJson.getString("errmsg"));
                }
            } else {
                log.error("获取微信access_token HTTP请求失败: {}", responseEntity.getStatusCode());
            }
        } catch (Exception e) {
            log.error("获取微信access_token异常: {}", e.getMessage(), e);
        }
        
        return null;
    }

    @Override
    public Map<String, Object> getCollegeBookStats() {
        String cacheKey = CACHE_STATS_PREFIX + "collegeBooks";
        Map<String, Object> cachedResult = redisCache.getCacheObject(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("status", 0); // 只统计在售图书

        Map<String, Object> booksResult = invokeCloudFunction("getAllPublishedBooks", params);
        
        if (booksResult != null && booksResult.containsKey("success") && (Boolean) booksResult.get("success")) {
            List<Map<String, Object>> books = (List<Map<String, Object>>) booksResult.get("data");
            
            // 初始化学院统计数据
            Map<String, String> collegeMap = new HashMap<>();
            collegeMap.put("0", "计算机");
            collegeMap.put("1", "地信");
            collegeMap.put("2", "环境");
            collegeMap.put("3", "经管");
            collegeMap.put("4", "材化");
            collegeMap.put("5", "英语");
            collegeMap.put("6", "地质");
            collegeMap.put("7", "珠宝");
            collegeMap.put("8", "自动化");
            collegeMap.put("9", "艺媒");
            collegeMap.put("10", "体育");
            collegeMap.put("11", "工程");
            collegeMap.put("12", "机电");
            collegeMap.put("13", "公管");
            collegeMap.put("14", "马克思");
            collegeMap.put("15", "海洋");
            collegeMap.put("16", "新能源");
            collegeMap.put("17", "其他");
            collegeMap.put("-1", "其他"); // 未分类的也算作其他

            // 统计各学院图书数量
            Map<String, Integer> collegeStats = new HashMap<>();
            for (String collegeId : collegeMap.keySet()) {
                collegeStats.put(collegeId, 0);
            }

            if (books != null) {
                for (Map<String, Object> book : books) {
                    String collegeId = String.valueOf(book.get("collegeid"));
                    if (collegeId == null) {
                        collegeId = "-1";
                    }
                    // 如果collegeId不在预定义的映射中，归类为"其他"
                    if (!collegeMap.containsKey(collegeId)) {
                        collegeId = "17";
                    }
                    collegeStats.put(collegeId, collegeStats.get(collegeId) + 1);
                }
            }

            // 构建返回数据
            List<Map<String, Object>> pieData = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : collegeStats.entrySet()) {
                if (entry.getValue() > 0) { // 只返回有图书的学院
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", collegeMap.get(entry.getKey()));
                    item.put("value", entry.getValue());
                    pieData.add(item);
                }
            }

            result.put("success", true);
            result.put("data", pieData);
            result.put("message", "获取学院图书统计成功");
        } else {
            result.put("success", false);
            result.put("message", "获取学院图书统计失败");
        }

        // 缓存结果
        redisCache.setCacheObject(cacheKey, result, STATISTICS_CACHE_SECONDS, TimeUnit.SECONDS);
        return result;
    }
} 