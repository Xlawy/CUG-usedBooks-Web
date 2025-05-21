package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
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
    @GetMapping("/all")
    public AjaxResult getAllStatistics()
    {
        return success(statisticsService.getAllStatistics());
    }

    /**
     * 获取用户统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @GetMapping("/users")
    public AjaxResult getUsersStatistics()
    {
        return success(statisticsService.getUsersCount());
    }

    /**
     * 获取订单统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @GetMapping("/orders")
    public AjaxResult getOrdersStatistics()
    {
        return success(statisticsService.getOrdersCount());
    }

    /**
     * 获取图书统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @GetMapping("/books")
    public AjaxResult getBooksStatistics()
    {
        return success(statisticsService.getBooksCount());
    }

    /**
     * 获取在售图书统计数据
     */
    @PreAuthorize("@ss.hasPermi('system:statistics:list')")
    @GetMapping("/publishedBooks")
    public AjaxResult getPublishedBooksStatistics()
    {
        return success(statisticsService.getPublishedBooksCount());
    }
} 