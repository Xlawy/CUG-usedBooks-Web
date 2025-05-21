package com.ruoyi.system.service;

import java.util.Map;

/**
 * 统计数据服务接口
 * 
 * @author ruoyi
 */
public interface IStatisticsService
{
    /**
     * 获取用户总数
     * 
     * @return 用户总数统计
     */
    public Map<String, Object> getUsersCount();

    /**
     * 获取订单总数
     * 
     * @return 订单总数统计
     */
    public Map<String, Object> getOrdersCount();

    /**
     * 获取图书总数
     * 
     * @return 图书总数统计
     */
    public Map<String, Object> getBooksCount();

    /**
     * 获取在售图书总数
     * 
     * @return 在售图书总数统计
     */
    public Map<String, Object> getPublishedBooksCount();
    
    /**
     * 获取所有统计数据
     * 
     * @return 包含所有统计项的统计数据
     */
    public Map<String, Object> getAllStatistics();
} 