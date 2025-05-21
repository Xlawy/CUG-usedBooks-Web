package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
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

        // 构建云函数查询参数
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("title", bookName);
        queryParams.put("pageNum", 1);
        queryParams.put("pageSize", 1); // 只需要一本匹配的书

        // 调用微信云函数查询图书详情，直接传递查询参数
        Map<String, Object> cloudFunctionResult = invokeCloudFunction("getAllBooks", queryParams);

        if (cloudFunctionResult != null && cloudFunctionResult.containsKey("success") && (Boolean) cloudFunctionResult.get("success")) {
            // 从结果中获取图书信息
            List<Map<String, Object>> books = (List<Map<String, Object>>) cloudFunctionResult.get("data");
            Map<String, Object> bookInfo = books != null && !books.isEmpty() ? books.get(0) : null;

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
                    case "summary":
                        fieldValue = bookInfo.get("summary");
                        fieldDesc = "摘要";
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
        String bookName = (String) parameters.get("bookName");

        Map<String, Object> result = new HashMap<>();

        if (StringUtils.isEmpty(orderId) && StringUtils.isEmpty(userId) && StringUtils.isEmpty(bookName)) {
            result.put("success", false);
            result.put("message", "未提供订单号、用户ID或图书名称");
            return result;
        }

        // 构建云函数查询参数
        Map<String, Object> queryParams = new HashMap<>();
        if (StringUtils.isNotEmpty(orderId)) {
            queryParams.put("orderId", orderId);
        }
        if (StringUtils.isNotEmpty(userId)) {
            queryParams.put("userId", userId);
        }
        if (StringUtils.isNotEmpty(bookName)) {
            queryParams.put("title", bookName); // 云函数使用title作为参数名
        }
        queryParams.put("pageNum", 1);
        queryParams.put("pageSize", 10);

        // 调用微信云函数查询订单，直接传递查询参数
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
                if (StringUtils.isNotEmpty(orderId)) {
                    result.put("message", String.format("未找到订单号为'%s'的订单", orderId));
                } else if (StringUtils.isNotEmpty(bookName)) {
                    result.put("message", String.format("未找到关于《%s》的订单", bookName));
                } else {
                    result.put("message", String.format("未找到用户'%s'的订单", userId));
                }
                return result;
            }

            result.put("success", true);
            result.put("data", orders);
            result.put("count", total);

            if (StringUtils.isNotEmpty(orderId)) {
                Map<String, Object> orderInfo = orders.get(0);
                result.put("message", String.format("订单%s的状态是: %s", orderId, orderInfo.get("status")));
            } else if (StringUtils.isNotEmpty(bookName)) {
                result.put("message", String.format("找到%d个关于《%s》的订单", total, bookName));
            } else {
                result.put("message", String.format("用户%s有%d个订单", userId, total));
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
        Boolean smartRecommend = (Boolean) parameters.get("smartRecommend");
        Integer collegeId = parameters.get("collegeId") instanceof Number ?
                            ((Number) parameters.get("collegeId")).intValue() : null;
        Integer kindId = parameters.get("kindId") instanceof Number ?
                        ((Number) parameters.get("kindId")).intValue() : null;

        Map<String, Object> result = new HashMap<>();

        // 获取所有图书数据
        Map<String, Object> booksResult = invokeCloudFunction("getAllBooks", new HashMap<>());
        List<Map<String, Object>> allBooks = null;

        if (booksResult != null && booksResult.containsKey("success") && (Boolean) booksResult.get("success")) {
            allBooks = (List<Map<String, Object>>) booksResult.get("data");
        }

        if (allBooks == null || allBooks.isEmpty()) {
            result.put("success", false);
            result.put("message", "没有可用的图书数据");
            return result;
        }

        // 现在我们有所有图书数据，开始进行推荐
        List<Map<String, Object>> recommendedBooks = new ArrayList<>();

        // 处理"all"类别为获取全部图书
        if ("all".equalsIgnoreCase(category)) {
            result.put("success", true);
            result.put("count", allBooks.size());
            result.put("data", allBooks);
            result.put("message", String.format("为您展示平台上的所有%d本图书", allBooks.size()));
            return result;
        }

        // 根据分类过滤图书
        if (allBooks != null && !allBooks.isEmpty()) {
            // 按collegeId和kindId过滤（专业和通用类别）
            if (collegeId != null || kindId != null) {
                for (Map<String, Object> book : allBooks) {
                    boolean matches = true;

                    // 按collegeId过滤
                    if (collegeId != null) {
                        Object bookCollegeId = book.get("collegeId");
                        if (bookCollegeId == null ||
                            !Integer.valueOf(collegeId).equals(
                                bookCollegeId instanceof Number ?
                                ((Number) bookCollegeId).intValue() : null)) {
                            matches = false;
                        }
                    }

                    // 按kindId过滤
                    if (matches && kindId != null) {
                        Object bookKindId = book.get("kindId");
                        if (bookKindId == null ||
                            !Integer.valueOf(kindId).equals(
                                bookKindId instanceof Number ?
                                ((Number) bookKindId).intValue() : null)) {
                            matches = false;
                        }
                    }

                    if (matches) {
                        recommendedBooks.add(book);
                    }
                }
            } else if (StringUtils.isNotEmpty(category)) {
                // 首先按分类过滤
                // 创建类别映射，支持多样化的用户输入
                Map<String, List<String>> categoryMapping = createCategoryMapping();

                // 获取匹配的类别关键词
                List<String> matchingKeywords = new ArrayList<>();
                for (Map.Entry<String, List<String>> entry : categoryMapping.entrySet()) {
                    for (String keyword : entry.getValue()) {
                        if (category.toLowerCase().contains(keyword.toLowerCase())) {
                            matchingKeywords.addAll(entry.getValue());
                            break;
                        }
                    }
                }

                // 使用关键词过滤
                if (!matchingKeywords.isEmpty()) {
                    for (Map<String, Object> book : allBooks) {
                        String bookCategory = (String) book.get("category");
                        String bookTitle = (String) book.get("title");
                        String bookDescription = (String) book.get("description");

                        boolean matched = false;

                        // 根据分类匹配
                        if (bookCategory != null) {
                            for (String keyword : matchingKeywords) {
                                if (bookCategory.toLowerCase().contains(keyword.toLowerCase())) {
                                    matched = true;
                                    break;
                                }
                            }
                        }

                        // 如果分类没匹配上，尝试从标题和描述中匹配
                        if (!matched) {
                            for (String keyword : matchingKeywords) {
                                if ((bookTitle != null && bookTitle.toLowerCase().contains(keyword.toLowerCase())) ||
                                    (bookDescription != null && bookDescription.toLowerCase().contains(keyword.toLowerCase()))) {
                                    matched = true;
                                    break;
                                }
                            }
                        }

                        if (matched) {
                            recommendedBooks.add(book);
                        }
                    }
                } else {
                    // 如果没有匹配的映射类别，直接用原始类别搜索
                    for (Map<String, Object> book : allBooks) {
                        String bookCategory = (String) book.get("category");
                        if (bookCategory != null && bookCategory.toLowerCase().contains(category.toLowerCase())) {
                            recommendedBooks.add(book);
                        }
                    }
                }
            } else {
                // 如果没有指定分类，复制所有图书
                recommendedBooks.addAll(allBooks);
            }

            // 如果结果较多，随机选择最多5本
            if (recommendedBooks.size() > 5) {
                List<Map<String, Object>> selectedBooks = new ArrayList<>();
                // 使用简单随机选择
                java.util.Random random = new java.util.Random();
                for (int i = 0; i < 5; i++) {
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
            if (collegeId != null) {
                String collegeName = getCollegeNameById(collegeId);
                result.put("message", String.format("没有找到%s学院的图书推荐", collegeName));
            } else if (StringUtils.isNotEmpty(category)) {
                result.put("message", String.format("没有找到%s类别的图书推荐", category));
            } else {
                result.put("message", "没有找到推荐图书");
            }
        } else {
            if (collegeId != null) {
                String collegeName = getCollegeNameById(collegeId);
                result.put("message", String.format("为您推荐%d本%s学院的图书", recommendedBooks.size(), collegeName));
            } else if (StringUtils.isNotEmpty(category)) {
                result.put("message", String.format("为您推荐%d本%s类别的图书", recommendedBooks.size(), category));
            } else {
                result.put("message", "为您推荐以下图书");
            }
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

            try {
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
                log.error("调用微信云函数HTTP请求异常: {}", e.getMessage(), e);
                // 返回一个模拟的结果，使请求不会完全失败
                return getSimulatedResult(functionName, params);
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
