package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.service.IStatisticsService;

/**
 * 系统数据统计Controller
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/statistics")
public class SysStatisticsController extends BaseController
{
    @Autowired
    private IStatisticsService statisticsService;

    /**
     * 获取所有统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @Log(title = "统计数据", businessType = BusinessType.OTHER)
    @GetMapping("/all")
    public AjaxResult getAllStatistics()
    {
        return success(statisticsService.getAllStatistics());
    }

    /**
     * 获取用户统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @Log(title = "用户统计", businessType = BusinessType.OTHER)
    @GetMapping("/users")
    public AjaxResult getUsersStatistics()
    {
        return success(statisticsService.getUsersCount());
    }

    /**
     * 获取订单统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @Log(title = "订单统计", businessType = BusinessType.OTHER)
    @GetMapping("/orders")
    public AjaxResult getOrdersStatistics()
    {
        return success(statisticsService.getOrdersCount());
    }

    /**
     * 获取图书统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @Log(title = "图书统计", businessType = BusinessType.OTHER)
    @GetMapping("/books")
    public AjaxResult getBooksStatistics()
    {
        return success(statisticsService.getBooksCount());
    }

    /**
     * 获取在售图书统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @Log(title = "在售图书统计", businessType = BusinessType.OTHER)
    @GetMapping("/publishedBooks")
    public AjaxResult getPublishedBooksStatistics()
    {
        return success(statisticsService.getPublishedBooksCount());
    }

    /**
     * 获取月度订单统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @Log(title = "月度订单统计", businessType = BusinessType.OTHER)
    @GetMapping("/orders/monthly/{yearMonth}")
    public AjaxResult getMonthlyOrdersStats(@PathVariable("yearMonth") String yearMonth)
    {
        return success(statisticsService.getMonthlyOrdersStats(yearMonth));
    }

    /**
     * 获取学院图书统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @Log(title = "学院图书统计", businessType = BusinessType.OTHER)
    @GetMapping("/books/college")
    public AjaxResult getCollegeBookStats()
    {
        return success(statisticsService.getCollegeBookStats());
    }
} 