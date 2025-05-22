package com.ruoyi.system.service.impl;

import java.util.*;

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
                case "published_book_search":
                    intentResult = processPublishedBookSearch(parameters);
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
                case "statistics":
                    intentResult = processStatistics(parameters);
                    break;
                case "order_create":
                    intentResult = processOrderCreate(parameters);
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

        // 处理学院分类
        Integer collegeId = null;
        if (StringUtils.isNotEmpty(category)) {
            Map<String, List<String>> categoryMapping = createCategoryMapping();
            // 遍历映射查找匹配的学院
            for (Map.Entry<String, List<String>> entry : categoryMapping.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (category.toLowerCase().contains(keyword.toLowerCase())) {
                        collegeId = getCollegeIdByName(entry.getKey());
                        break;
                    }
                }
                if (collegeId != null) break;
            }
        }

        // 构建云函数查询参数
        Map<String, Object> queryParams = new HashMap<>();
        if (StringUtils.isNotEmpty(bookName)) {
            queryParams.put("title", bookName);
        }
        if (StringUtils.isNotEmpty(author)) {
            queryParams.put("author", author);
        }
        if (StringUtils.isNotEmpty(isbn)) {
            queryParams.put("isbn", isbn);
        }
        if (collegeId != null) {
            queryParams.put("collegeId", collegeId);
        }

        // 可以添加页码和每页条数
        queryParams.put("pageNum", 1);
        queryParams.put("pageSize", 20);

        // 调用微信云函数查询图书，直接传递查询参数
        Map<String, Object> cloudFunctionResult = invokeCloudFunction("getAllBooks", queryParams);

        Map<String, Object> result = new HashMap<>();

        if (cloudFunctionResult != null && cloudFunctionResult.containsKey("success") && (Boolean) cloudFunctionResult.get("success")) {
            List<Map<String, Object>> books = (List<Map<String, Object>>) cloudFunctionResult.get("data");
            int total = 0;
            if (cloudFunctionResult.containsKey("total")) {
                total = ((Number) cloudFunctionResult.get("total")).intValue();
            } else if (books != null) {
                total = books.size();
            }

            result.put("success", true);
            result.put("count", total);
            result.put("data", books);

            if (books == null || books.isEmpty()) {
                result.put("message", "未找到符合条件的图书");
            } else {
                result.put("message", String.format("找到%d本符合条件的图书", total));
            }
        } else {
            result.put("success", false);
            result.put("message", cloudFunctionResult != null ? cloudFunctionResult.get("message") : "查询图书失败");
        }

        return result;
    }

    /**
     * 根据学院名称获取ID
     */
    private Integer getCollegeIdByName(String collegeName) {
        switch (collegeName) {
            case "计算机学院": return 0;
            case "地信学院": return 1;
            case "环境学院": return 2;
            case "经管学院": return 3;
            case "材化学院": return 4;
            case "外国语学院": return 5;
            case "地质学院": return 6;
            case "珠宝学院": return 7;
            case "自动化学院": return 8;
            case "艺媒学院": return 9;
            case "体育学院": return 10;
            case "工程学院": return 11;
            case "机电学院": return 12;
            case "公管学院": return 13;
            case "马克思学院": return 14;
            case "海洋学院": return 15;
            case "新能源学院": return 16;
            case "其他学院": return 17;
            default: return null;
        }
    }

    /**
     * 处理图书详情意图
     */
    @Override
    public Map<String, Object> processBookInfo(Map<String, Object> parameters)
    {
        Map<String, Object> result = new HashMap<>();
        String bookName = (String) parameters.get("bookName");
        String isbn = (String) parameters.get("isbn");
        String field = (String) parameters.get("field");

        if (StringUtils.isEmpty(bookName) && StringUtils.isEmpty(isbn)) {
            result.put("success", false);
            result.put("message", "请提供图书名称或ISBN");
            return result;
        }

        // 构建查询参数
        Map<String, Object> queryParams = new HashMap<>();
        if (StringUtils.isNotEmpty(bookName)) {
            queryParams.put("title", bookName);
        }
        if (StringUtils.isNotEmpty(isbn)) {
            queryParams.put("isbn", isbn);
        }
        queryParams.put("pageNum", 1);
        queryParams.put("pageSize", 1);

        // 调用微信云函数查询图书详情
        Map<String, Object> cloudFunctionResult = invokeCloudFunction("getAllBooks", queryParams);

        if (cloudFunctionResult != null && cloudFunctionResult.containsKey("success") && (Boolean) cloudFunctionResult.get("success")) {
            List<Map<String, Object>> books = (List<Map<String, Object>>) cloudFunctionResult.get("data");
            Map<String, Object> bookInfo = books != null && !books.isEmpty() ? books.get(0) : null;

            if (bookInfo == null || bookInfo.isEmpty()) {
                result.put("success", false);
                result.put("message", String.format("未找到相关图书信息"));
                return result;
            }

            // 如果指定了特定字段，只返回该字段的信息
            if (StringUtils.isNotEmpty(field)) {
                Object fieldValue = null;
                String fieldDesc = "";

                switch (field.toLowerCase()) {
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
                    case "author":
                        fieldValue = bookInfo.get("author");
                        fieldDesc = "作者";
                        break;
                    case "isbn":
                        fieldValue = bookInfo.get("isbn");
                        fieldDesc = "ISBN";
                        break;
                    case "condition":
                        fieldValue = bookInfo.get("condition");
                        fieldDesc = "图书成色";
                        break;
                    case "description":
                        fieldValue = bookInfo.get("description");
                        fieldDesc = "图书描述";
                        break;
                    case "college":
                        Integer collegeId = (Integer) bookInfo.get("collegeId");
                        fieldValue = getCollegeNameById(collegeId);
                        fieldDesc = "所属学院";
                        break;
                    default:
                        result.put("success", false);
                        result.put("message", "不支持查询的字段: " + field);
                        return result;
                }

                result.put("success", true);
                result.put("field", field);
                result.put("fieldDesc", fieldDesc);
                result.put("value", fieldValue);
                result.put("message", String.format("《%s》的%s是: %s",
                    bookInfo.get("title"), fieldDesc, fieldValue));
            } else {
                // 返回完整的图书信息
                result.put("success", true);
                result.put("data", bookInfo);

                // 添加学院名称
                Integer collegeId = (Integer) bookInfo.get("collegeId");
                bookInfo.put("collegeName", getCollegeNameById(collegeId));

                // 格式化消息
                StringBuilder message = new StringBuilder();
                message.append(String.format("《%s》的详细信息：\n", bookInfo.get("title")));
                message.append(String.format("作者：%s\n", bookInfo.get("author")));
                message.append(String.format("出版社：%s\n", bookInfo.get("publisher")));
                message.append(String.format("ISBN：%s\n", bookInfo.get("isbn")));
                message.append(String.format("价格：%.2f元\n", bookInfo.get("price")));
                message.append(String.format("库存：%d\n", bookInfo.get("stock")));
                message.append(String.format("成色：%s\n", bookInfo.get("condition")));
                message.append(String.format("所属学院：%s", bookInfo.get("collegeName")));

                result.put("message", message.toString());
            }
        } else {
            result.put("success", false);
            result.put("message", "获取图书详情失败");
        }

        return result;
    }

    /**
     * 处理订单状态查询意图
     */
    @Override
    public Map<String, Object> processOrderStatus(Map<String, Object> parameters)
    {
        Map<String, Object> result = new HashMap<>();
        String orderId = (String) parameters.get("orderId");
        String userId = (String) parameters.get("userId");
        String bookName = (String) parameters.get("bookName");
        String status = (String) parameters.get("status");
        String timeRange = (String) parameters.get("timeRange");

        // 构建查询参数
        Map<String, Object> queryParams = new HashMap<>();

        if (StringUtils.isNotEmpty(orderId)) {
            queryParams.put("orderId", orderId);
        }
        if (StringUtils.isNotEmpty(userId)) {
            queryParams.put("userId", userId);
        }
        if (StringUtils.isNotEmpty(bookName)) {
            queryParams.put("title", bookName);
        }
        if (StringUtils.isNotEmpty(status)) {
            queryParams.put("status", status);
        }
        if (StringUtils.isNotEmpty(timeRange)) {
            // 解析时间范围，格式如：today, week, month, year
            switch(timeRange.toLowerCase()) {
                case "today":
                    queryParams.put("startTime", System.currentTimeMillis() - 24 * 60 * 60 * 1000);
                    break;
                case "week":
                    queryParams.put("startTime", System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
                    break;
                case "month":
                    queryParams.put("startTime", System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
                    break;
                case "year":
                    queryParams.put("startTime", System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000);
                    break;
            }
        }

        queryParams.put("pageNum", 1);
        queryParams.put("pageSize", 10);

        // 调用微信云函数查询订单
        Map<String, Object> cloudFunctionResult = invokeCloudFunction("getAllOrders", queryParams);

        if (cloudFunctionResult != null && cloudFunctionResult.containsKey("success") && (Boolean) cloudFunctionResult.get("success")) {
            List<Map<String, Object>> orders = (List<Map<String, Object>>) cloudFunctionResult.get("data");
            int total = 0;
            if (cloudFunctionResult.containsKey("total")) {
                total = ((Number) cloudFunctionResult.get("total")).intValue();
            } else if (orders != null) {
                total = orders.size();
            }

            if (orders == null || orders.isEmpty()) {
                result.put("success", false);
                StringBuilder message = new StringBuilder("未找到");
                if (StringUtils.isNotEmpty(timeRange)) {
                    message.append(getTimeRangeDesc(timeRange)).append("的");
                }
                if (StringUtils.isNotEmpty(orderId)) {
                    message.append("订单号为'").append(orderId).append("'的");
                }
                if (StringUtils.isNotEmpty(bookName)) {
                    message.append("关于《").append(bookName).append("》的");
                }
                if (StringUtils.isNotEmpty(status)) {
                    message.append(getStatusDesc(status)).append("的");
                }
                message.append("订单");
                result.put("message", message.toString());
                return result;
            }

            result.put("success", true);
            result.put("data", orders);
            result.put("count", total);

            // 构建返回消息
            StringBuilder message = new StringBuilder();
            if (StringUtils.isNotEmpty(orderId)) {
                Map<String, Object> orderInfo = orders.get(0);
                message.append(String.format("订单%s的状态是: %s", orderId, getStatusDesc((String)orderInfo.get("status"))));
                // 添加订单详细信息
                message.append("\n订单信息：");
                message.append(String.format("\n创建时间：%s", orderInfo.get("createTime")));
                message.append(String.format("\n更新时间：%s", orderInfo.get("updateTime")));
                message.append(String.format("\n订单金额：%.2f元", orderInfo.get("totalAmount")));
                if (orderInfo.get("payTime") != null) {
                    message.append(String.format("\n支付时间：%s", orderInfo.get("payTime")));
                }
            } else {
                message.append("找到");
                if (StringUtils.isNotEmpty(timeRange)) {
                    message.append(getTimeRangeDesc(timeRange));
                }
                if (StringUtils.isNotEmpty(status)) {
                    message.append(getStatusDesc(status));
                }
                if (StringUtils.isNotEmpty(bookName)) {
                    message.append(String.format("关于《%s》的", bookName));
                }
                message.append(String.format("%d个订单", total));

                // 添加订单状态统计
                Map<String, Integer> statusCount = new HashMap<>();
                for (Map<String, Object> order : orders) {
                    String orderStatus = (String) order.get("status");
                    statusCount.put(orderStatus, statusCount.getOrDefault(orderStatus, 0) + 1);
                }
                message.append("\n订单状态统计：");
                for (Map.Entry<String, Integer> entry : statusCount.entrySet()) {
                    message.append(String.format("\n%s：%d个", getStatusDesc(entry.getKey()), entry.getValue()));
                }
            }

            result.put("message", message.toString());
        } else {
            result.put("success", false);
            result.put("message", "获取订单状态失败");
        }

        return result;
    }

    /**
     * 获取订单状态描述
     */
    private String getStatusDesc(String status) {
        switch (status.toLowerCase()) {
            case "pending":
                return "待支付";
            case "paid":
                return "已支付";
            case "shipping":
                return "配送中";
            case "completed":
                return "已完成";
            case "cancelled":
                return "已取消";
            case "refunding":
                return "退款中";
            case "refunded":
                return "已退款";
            default:
                return status;
        }
    }

    /**
     * 获取时间范围描述
     */
    private String getTimeRangeDesc(String timeRange) {
        switch (timeRange.toLowerCase()) {
            case "today":
                return "今天";
            case "week":
                return "本周";
            case "month":
                return "本月";
            case "year":
                return "今年";
            default:
                return timeRange;
        }
    }

    /**
     * 处理图书推荐意图
     */
    @Override
    public Map<String, Object> processBookRecommendation(Map<String, Object> parameters)
    {
        Map<String, Object> result = new HashMap<>();
        String category = (String) parameters.get("category");
        String userId = (String) parameters.get("userId");
        String recommendType = (String) parameters.get("type"); // hot, new, similar
        Integer collegeId = null;

        // 获取所有图书
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("pageNum", 1);
        queryParams.put("pageSize", 50); // 获取足够多的图书用于推荐

        // 处理学院分类
        if (StringUtils.isNotEmpty(category)) {
            // 尝试从类别映射中找到对应的学院
            Map<String, List<String>> categoryMapping = createCategoryMapping();
            for (Map.Entry<String, List<String>> entry : categoryMapping.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (category.toLowerCase().contains(keyword.toLowerCase())) {
                        collegeId = getCollegeIdByName(entry.getKey());
                        break;
                    }
                }
                if (collegeId != null) break;
            }

            if (collegeId != null) {
                queryParams.put("collegeId", collegeId);
            }
        }

        // 调用微信云函数获取图书列表
        Map<String, Object> cloudFunctionResult = invokeCloudFunction("getAllBooks", queryParams);

        if (cloudFunctionResult != null && cloudFunctionResult.containsKey("success") && (Boolean) cloudFunctionResult.get("success")) {
            List<Map<String, Object>> allBooks = (List<Map<String, Object>>) cloudFunctionResult.get("data");

            if (allBooks == null || allBooks.isEmpty()) {
                result.put("success", false);
                result.put("message", "暂无可推荐的图书");
                return result;
            }

            List<Map<String, Object>> recommendedBooks = new ArrayList<>();
            String recommendReason = "为您推荐";

            // 根据推荐类型进行筛选
            if ("hot".equalsIgnoreCase(recommendType)) {
                // 按销量排序
                allBooks.sort((a, b) -> {
                    Integer salesA = (Integer) a.getOrDefault("sales", 0);
                    Integer salesB = (Integer) b.getOrDefault("sales", 0);
                    return salesB.compareTo(salesA);
                });
                recommendedBooks = allBooks.subList(0, Math.min(5, allBooks.size()));
                recommendReason = "热门图书推荐";
            } else if ("new".equalsIgnoreCase(recommendType)) {
                // 按上架时间排序
                allBooks.sort((a, b) -> {
                    Long timeA = (Long) a.getOrDefault("createTime", 0L);
                    Long timeB = (Long) b.getOrDefault("createTime", 0L);
                    return timeB.compareTo(timeA);
                });
                recommendedBooks = allBooks.subList(0, Math.min(5, allBooks.size()));
                recommendReason = "新上架图书推荐";
            } else if ("similar".equalsIgnoreCase(recommendType) && StringUtils.isNotEmpty(userId)) {
                // 基于用户历史订单推荐相似图书
                Map<String, Object> orderParams = new HashMap<>();
                orderParams.put("userId", userId);
                Map<String, Object> orderResult = invokeCloudFunction("getAllOrders", orderParams);

                if (orderResult != null && orderResult.containsKey("success") &&
                    (Boolean) orderResult.get("success")) {
                    List<Map<String, Object>> userOrders = (List<Map<String, Object>>) orderResult.get("data");

                    if (userOrders != null && !userOrders.isEmpty()) {
                        // 获取用户购买过的图书类别
                        Set<Integer> userCollegeIds = new HashSet<>();
                        for (Map<String, Object> order : userOrders) {
                            Map<String, Object> book = (Map<String, Object>) order.get("book");
                            if (book != null && book.containsKey("collegeId")) {
                                userCollegeIds.add((Integer) book.get("collegeId"));
                            }
                        }

                        // 推荐相同类别的其他图书
                        for (Map<String, Object> book : allBooks) {
                            Integer bookCollegeId = (Integer) book.get("collegeId");
                            if (bookCollegeId != null && userCollegeIds.contains(bookCollegeId)) {
                                recommendedBooks.add(book);
                                if (recommendedBooks.size() >= 5) break;
                            }
                        }
                        recommendReason = "基于您的购买历史推荐";
                    }
                }

                // 如果没有找到相似图书，返回热门图书
                if (recommendedBooks.isEmpty()) {
                    allBooks.sort((a, b) -> {
                        Integer salesA = (Integer) a.getOrDefault("sales", 0);
                        Integer salesB = (Integer) b.getOrDefault("sales", 0);
                        return salesB.compareTo(salesA);
                    });
                    recommendedBooks = allBooks.subList(0, Math.min(5, allBooks.size()));
                    recommendReason = "热门图书推荐";
                }
            } else {
                // 默认推荐
                recommendedBooks = allBooks.subList(0, Math.min(5, allBooks.size()));
                recommendReason = "为您推荐";
            }

            // 构建推荐结果
            result.put("success", true);
            result.put("data", recommendedBooks);
            result.put("count", recommendedBooks.size());

            // 构建推荐消息
            StringBuilder message = new StringBuilder();
            message.append(recommendReason).append("：\n");
            for (int i = 0; i < recommendedBooks.size(); i++) {
                Map<String, Object> book = recommendedBooks.get(i);
                message.append(String.format("%d. 《%s》 - %s\n",
                    i + 1,
                    book.get("title"),
                    book.get("author")));
                message.append(String.format("   价格：%s元", book.get("price")));
                if (book.containsKey("sales")) {
                    message.append(String.format(" | 已售：%d本", book.get("sales")));
                }
                message.append("\n");
            }

            // 添加分类信息
            if (collegeId != null) {
                message.append(String.format("\n以上图书均来自%s", getCollegeNameById(collegeId)));
            }

            result.put("message", message.toString());
        } else {
            result.put("success", false);
            result.put("message", "获取推荐图书失败");
        }

        return result;
    }

    /**
     * 创建类别映射表
     */
    private Map<String, List<String>> createCategoryMapping() {
        Map<String, List<String>> mapping = new HashMap<>();

        // 学科类别映射
        mapping.put("计算机", Arrays.asList("计算机", "编程", "软件", "开发", "java", "python", "c++", "算法", "编码", "数据结构", "程序", "网络", "安全"));
        mapping.put("数学", Arrays.asList("数学", "高数", "线代", "线性代数", "微积分", "离散", "概率", "统计"));
        mapping.put("文学", Arrays.asList("文学", "小说", "诗歌", "散文", "戏剧", "文集", "名著"));
        mapping.put("历史", Arrays.asList("历史", "考古", "文明", "古代", "近代", "革命", "战争", "传记", "纪实"));
        mapping.put("艺术", Arrays.asList("艺术", "音乐", "绘画", "设计", "建筑", "摄影", "电影", "舞蹈", "美术"));
        mapping.put("科学", Arrays.asList("科学", "物理", "化学", "生物", "地质", "天文", "医学", "宇宙", "科普"));
        mapping.put("经济", Arrays.asList("经济", "金融", "投资", "股票", "基金", "理财", "会计", "管理", "商业"));
        mapping.put("教育", Arrays.asList("教育", "考研", "考试", "教材", "学习", "教学", "课程", "课本"));

        // CUG专业学院类别映射
        mapping.put("计算机学院", Arrays.asList("计算机", "计算机学院", "计科", "软工", "网安", "人工智能", "大数据"));
        mapping.put("地信学院", Arrays.asList("地信", "地信学院", "地理信息", "遥感", "测绘", "导航", "地图"));
        mapping.put("环境学院", Arrays.asList("环境", "环境学院", "环境工程", "生态", "水文", "大气", "污染"));
        mapping.put("经管学院", Arrays.asList("经管", "经管学院", "经济", "管理", "会计", "金融", "市场营销"));
        mapping.put("材化学院", Arrays.asList("材化", "材化学院", "材料", "化学", "高分子", "无机"));
        mapping.put("外国语学院", Arrays.asList("英语", "外国语学院", "外语", "翻译", "日语", "法语", "德语"));
        mapping.put("地质学院", Arrays.asList("地质", "地质学院", "地质学", "岩石", "矿物", "古生物"));
        mapping.put("珠宝学院", Arrays.asList("珠宝", "珠宝学院", "宝石", "首饰", "玉石", "鉴定"));
        mapping.put("自动化学院", Arrays.asList("自动化", "自动化学院", "控制", "电气", "机器人"));
        mapping.put("艺媒学院", Arrays.asList("艺媒", "艺媒学院", "艺术", "设计", "传媒", "动画"));
        mapping.put("体育学院", Arrays.asList("体育", "体育学院", "运动", "健身", "竞技"));
        mapping.put("工程学院", Arrays.asList("工程", "工程学院", "土木", "建筑", "工程力学"));
        mapping.put("机电学院", Arrays.asList("机电", "机电学院", "机械", "电子", "汽车"));
        mapping.put("公管学院", Arrays.asList("公管", "公管学院", "公共管理", "政治", "行政"));
        mapping.put("马克思学院", Arrays.asList("马克思", "马克思学院", "哲学", "政治学", "思想"));
        mapping.put("海洋学院", Arrays.asList("海洋", "海洋学院", "海洋科学", "海洋地质"));
        mapping.put("新能源学院", Arrays.asList("新能源", "新能源学院", "能源", "电力", "可再生"));
        mapping.put("李四光学院", Arrays.asList("李四光", "李四光学院", "基础科学"));
        mapping.put("其他学院", Arrays.asList("其他", "通识", "公共课"));

        return mapping;
    }

    /**
     * 根据学院ID获取学院名称
     */
    private String getCollegeNameById(Integer collegeId) {
        if (collegeId == null) return "未知";

        switch (collegeId) {
            case 0: return "计算机";
            case 1: return "地信";
            case 2: return "环境";
            case 3: return "经管";
            case 4: return "材化";
            case 5: return "英语";
            case 6: return "地质";
            case 7: return "珠宝";
            case 8: return "自动化";
            case 9: return "艺媒";
            case 10: return "体育";
            case 11: return "工程";
            case 12: return "机电";
            case 13: return "公管";
            case 14: return "马克思";
            case 15: return "海洋";
            case 16: return "新能源/李四光";
            case 17: return "其他";
            default: return "未知学院";
        }
    }

    /**
     * 处理发布图书搜索意图
     */
    public Map<String, Object> processPublishedBookSearch(Map<String, Object> parameters)
    {
        String bookName = (String) parameters.get("bookName");
        String author = (String) parameters.get("author");
        String isbn = (String) parameters.get("isbn");
        String category = (String) parameters.get("category");
        boolean listAll = bookName == null && author == null && isbn == null &&
                         (parameters.containsKey("listAll") ||
                          Boolean.TRUE.equals(parameters.get("listAll")));

        // 构建云函数查询参数
        Map<String, Object> queryParams = new HashMap<>();
        if (StringUtils.isNotEmpty(bookName)) {
            queryParams.put("title", bookName);
        }
        if (StringUtils.isNotEmpty(author)) {
            queryParams.put("author", author);
        }
        if (StringUtils.isNotEmpty(isbn)) {
            queryParams.put("isbn", isbn);
        }
        // 默认查询状态为在售的书籍
        queryParams.put("status", 0);

        // 调用微信云函数查询发布的图书，传递查询参数
        Map<String, Object> cloudFunctionResult = invokeCloudFunction("getAllPublishedBooks", queryParams);

        Map<String, Object> result = new HashMap<>();

        if (cloudFunctionResult != null && cloudFunctionResult.containsKey("success") && (Boolean) cloudFunctionResult.get("success")) {
            List<Map<String, Object>> books = (List<Map<String, Object>>) cloudFunctionResult.get("data");
            int total = 0;
            if (cloudFunctionResult.containsKey("total")) {
                total = ((Number) cloudFunctionResult.get("total")).intValue();
            } else if (books != null) {
                total = books.size();
            }

            result.put("success", true);
            result.put("count", total);
            result.put("data", books);

            if (books == null || books.isEmpty()) {
                result.put("message", "未找到符合条件的在售图书");
            } else {
                result.put("message", String.format("找到%d本符合条件的在售图书", total));
            }
        } else {
            result.put("success", false);
            result.put("message", cloudFunctionResult != null ? cloudFunctionResult.get("message") : "查询在售图书失败");
        }

        return result;
    }

    /**
     * 处理信息统计意图
     */
    public Map<String, Object> processStatistics(Map<String, Object> parameters)
    {
        String statType = (String) parameters.get("statType");
        String timeRange = (String) parameters.get("timeRange");

        Map<String, Object> result = new HashMap<>();

        // 参数标准化处理
        if ("totalUsers".equals(statType)) {
            statType = "users";
        }

        // 根据统计类型进行不同的处理
        switch (statType) {
            case "users":
                // 获取用户统计
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
                break;

            case "orders":
                // 获取订单统计
                Map<String, Object> ordersResult = invokeCloudFunction("getAllOrders", new HashMap<>());
                if (ordersResult != null && ordersResult.containsKey("success") && (Boolean) ordersResult.get("success")) {
                    List<Map<String, Object>> orders = (List<Map<String, Object>>) ordersResult.get("data");
                    int orderCount = orders != null ? orders.size() : 0;

                    // 从total字段获取总数，如果存在
                    if (ordersResult.containsKey("total")) {
                        orderCount = ((Number) ordersResult.get("total")).intValue();
                    }

                    // 如果指定了时间范围，进行过滤
                    if (StringUtils.isNotEmpty(timeRange)) {
                        // 简单实现，实际应该根据具体的时间范围格式进行解析
                        result.put("timeRange", timeRange);
                    }

                    result.put("success", true);
                    result.put("statType", "orders");
                    result.put("count", orderCount);
                    result.put("message", String.format("平台当前共有%d个订单", orderCount));
                } else {
                    result.put("success", false);
                    result.put("message", "获取订单统计信息失败");
                }
                break;

            case "books":
                // 获取图书统计
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
                break;

            case "publishedBooks":
                // 获取发布图书统计
                Map<String, Object> publishedBooksResult = invokeCloudFunction("getAllPublishedBooks", new HashMap<>());
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
                break;

            default:
                result.put("success", false);
                result.put("message", "不支持的统计类型: " + statType);
                break;
        }

        return result;
    }

    /**
     * 处理创建订单意图
     */
    public Map<String, Object> processOrderCreate(Map<String, Object> parameters)
    {
        String bookName = (String) parameters.get("bookName");
        String userId = (String) parameters.get("userId");

        Map<String, Object> result = new HashMap<>();

        // 验证必要参数
        if (StringUtils.isEmpty(bookName)) {
            result.put("success", false);
            result.put("message", "创建订单需要指定图书名称");
            return result;
        }

        // 在实际应用中，可能需要检查图书是否存在，用户是否有效等
        // 这里只给出指导信息
        result.put("success", true);
        result.put("intent", "order_create");
        result.put("message", String.format("要购买《%s》，请前往图书详情页面点击'购买'按钮创建订单", bookName));

        // 添加指导步骤
        List<String> steps = new ArrayList<>();
        steps.add("在首页搜索栏输入图书名称");
        steps.add("在搜索结果中找到想要购买的图书");
        steps.add("点击图书封面进入详情页");
        steps.add("点击'加入购物车'或'立即购买'按钮");
        steps.add("确认订单信息并提交");

        result.put("steps", steps);

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
        Map<String, Object> result = null;
        Exception exception = null;

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
                        result = new HashMap<>();
                        result.put("success", false);
                        result.put("message", "请求的云函数'" + functionName + "'不存在");
                        return result;
                }
                log.info("已将请求重定向到云函数: {}", functionName);
            }

            // 获取微信小程序的access_token
            String accessToken = getWxAccessToken(false);
            if (StringUtils.isEmpty(accessToken)) {
                log.error("无法获取微信小程序access_token");
                result = new HashMap<>();
                result.put("success", false);
                result.put("message", "无法获取微信接口访问凭证");
                return result;
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
                        result = JSON.parseObject(respData, Map.class);
                        if (result == null) {
                            result = new HashMap<>();
                            result.put("success", true);
                            result.put("data", new ArrayList<>());
                            result.put("total", 0);
                        }
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
                                result = JSON.parseObject(respData, Map.class);
                                if (result == null) {
                                    result = new HashMap<>();
                                    result.put("success", true);
                                    result.put("data", new ArrayList<>());
                                    result.put("total", 0);
                                }
                            }
                        }
                        if (result == null) {
                            log.error("微信云函数调用失败，即使刷新token后也失败: {}", responseJson.getString("errmsg"));
                            result = new HashMap<>();
                            result.put("success", false);
                            result.put("message", "调用云函数失败: " + responseJson.getString("errmsg"));
                        }
                    } else {
                        log.error("微信云函数调用失败: {}", responseJson.getString("errmsg"));
                        result = new HashMap<>();
                        result.put("success", false);
                        result.put("message", "调用云函数失败: " + responseJson.getString("errmsg"));
                    }
                } else {
                    log.error("微信云函数调用HTTP请求失败: {}", responseEntity.getStatusCode());
                    result = new HashMap<>();
                    result.put("success", false);
                    result.put("message", "调用云函数HTTP请求失败: " + responseEntity.getStatusCode());
                }
            } catch (Exception e) {
                log.error("调用微信云函数HTTP请求异常: {}", e.getMessage(), e);
                exception = e;
                result = new HashMap<>();
                result.put("success", false);
                result.put("message", "调用云函数HTTP请求异常: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("调用微信云函数处理异常: {}", e.getMessage(), e);
            exception = e;
            result = new HashMap<>();
            result.put("success", false);
            result.put("message", "调用云函数异常: " + e.getMessage());
        } finally {
            // 记录操作日志
            com.ruoyi.system.utils.CloudFunctionLogUtils.recordCloudFunctionLog(functionName, params, result, exception);
        }

        return result;
    }

    /**
     * 获取模拟的云函数调用结果
     * 用于在实际调用失败时返回一些测试数据
     */
    private Map<String, Object> getSimulatedResult(String functionName, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);

        if ("getAllBooks".equals(functionName)) {
            List<Map<String, Object>> books = new ArrayList<>();

            // 添加一些模拟图书数据
            Map<String, Object> book1 = new HashMap<>();
            book1.put("id", "1");
            book1.put("title", "Java编程思想");
            book1.put("author", "Bruce Eckel");
            book1.put("publisher", "机械工业出版社");
            book1.put("price", 108.00);
            book1.put("stock", 10);
            book1.put("isbn", "9787111213826");
            book1.put("category", "计算机");
            book1.put("description", "Java学习经典，全面介绍Java编程的各个方面。");

            Map<String, Object> book2 = new HashMap<>();
            book2.put("id", "2");
            book2.put("title", "离散数学");
            book2.put("author", "屈婉玲");
            book2.put("publisher", "高等教育出版社");
            book2.put("price", 59.00);
            book2.put("stock", 5);
            book2.put("isbn", "9787040455243");
            book2.put("category", "数学");
            book2.put("description", "计算机专业基础课程教材，讲解离散结构和数学方法。");

            Map<String, Object> book3 = new HashMap<>();
            book3.put("id", "3");
            book3.put("title", "算法导论");
            book3.put("author", "Thomas H. Cormen");
            book3.put("publisher", "机械工业出版社");
            book3.put("price", 128.00);
            book3.put("stock", 3);
            book3.put("isbn", "9787111407010");
            book3.put("category", "计算机");
            book3.put("description", "全面介绍算法设计与分析的经典教材。");

            books.add(book1);
            books.add(book2);
            books.add(book3);

            result.put("data", books);
            result.put("message", "模拟数据：获取了3本图书");

        } else if ("getAllOrders".equals(functionName)) {
            List<Map<String, Object>> orders = new ArrayList<>();

            // 添加一些模拟订单数据
            Map<String, Object> order1 = new HashMap<>();
            order1.put("orderId", "12345");
            order1.put("userId", "10001");
            order1.put("status", "已完成");
            order1.put("totalAmount", 108.00);
            order1.put("createTime", "2023-06-15 14:30:45");

            Map<String, Object> order2 = new HashMap<>();
            order2.put("orderId", "12346");
            order2.put("userId", "10001");
            order2.put("status", "待发货");
            order2.put("totalAmount", 59.00);
            order2.put("createTime", "2023-06-18 09:12:33");

            orders.add(order1);
            orders.add(order2);

            result.put("data", orders);
            result.put("message", "模拟数据：获取了2个订单");

        } else {
            // 默认返回空结果
            result.put("data", new ArrayList<>());
            result.put("message", "模拟数据：无数据");
        }

        return result;
    }

    /**
     * 获取微信小程序access_token
     *
     * @return access_token
     */
    private String getWxAccessToken(boolean forceRefresh) {
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
