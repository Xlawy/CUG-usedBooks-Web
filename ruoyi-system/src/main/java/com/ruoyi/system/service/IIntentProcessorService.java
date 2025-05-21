package com.ruoyi.system.service;

import java.util.Map;

/**
 * 意图处理服务接口
 * 处理AI识别出的不同类型意图，执行相应的查询或操作
 * 
 * @author ruoyi
 */
public interface IIntentProcessorService
{
    /**
     * 处理意图
     * 
     * @param intent 意图类型
     * @param parameters 意图参数
     * @return 处理结果
     */
    public Map<String, Object> processIntent(String intent, Map<String, Object> parameters);
    
    /**
     * 处理图书搜索意图
     * 
     * @param parameters 搜索参数
     * @return 搜索结果
     */
    public Map<String, Object> processBookSearch(Map<String, Object> parameters);
    
    /**
     * 处理图书详情意图
     * 
     * @param parameters 查询参数
     * @return 图书详情
     */
    public Map<String, Object> processBookInfo(Map<String, Object> parameters);
    
    /**
     * 处理订单状态查询意图
     * 
     * @param parameters 查询参数
     * @return 订单状态信息
     */
    public Map<String, Object> processOrderStatus(Map<String, Object> parameters);
    
    /**
     * 处理图书推荐意图
     * 
     * @param parameters 推荐参数
     * @return 推荐图书列表
     */
    public Map<String, Object> processBookRecommendation(Map<String, Object> parameters);
} 