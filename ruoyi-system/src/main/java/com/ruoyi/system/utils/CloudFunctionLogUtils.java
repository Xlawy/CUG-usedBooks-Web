package com.ruoyi.system.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.domain.SysOperLog;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.BusinessStatus;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.service.ISysOperLogService;

import java.util.Map;

/**
 * 云函数操作日志记录工具类
 * 
 * @author ruoyi
 */
public class CloudFunctionLogUtils
{
    private static final Logger log = LoggerFactory.getLogger(CloudFunctionLogUtils.class);

    /**
     * 记录云函数操作日志
     * 
     * @param functionName 云函数名称
     * @param params 请求参数
     * @param result 执行结果
     * @param e 异常信息
     */
    public static void recordCloudFunctionLog(String functionName, Map<String, Object> params, Map<String, Object> result, Exception e)
    {
        try
        {
            // 创建操作日志对象
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            
            // 设置模块标题
            operLog.setTitle("云函数调用");
            
            // 设置方法名称
            operLog.setMethod(functionName);
            
            // 设置请求方式
            operLog.setRequestMethod("POST");
            
            // 设置操作类别
            if (functionName.startsWith("get") || functionName.startsWith("search")) {
                operLog.setBusinessType(BusinessType.OTHER.ordinal());
            } else if (functionName.startsWith("add")) {
                operLog.setBusinessType(BusinessType.INSERT.ordinal());
            } else if (functionName.startsWith("update")) {
                operLog.setBusinessType(BusinessType.UPDATE.ordinal());
            } else if (functionName.startsWith("delete")) {
                operLog.setBusinessType(BusinessType.DELETE.ordinal());
            } else {
                operLog.setBusinessType(BusinessType.OTHER.ordinal());
            }
            
            // 设置请求URL
            operLog.setOperUrl("/api/wx/cloudfunction/" + functionName);
            
            // 设置请求参数
            operLog.setOperParam(params != null ? params.toString() : "");
            
            // 设置操作地点
            operLog.setOperIp(IpUtils.getIpAddr());
            
            // 返回参数
            if (result != null)
            {
                operLog.setJsonResult(result.toString());
            }

            // 设置操作状态
            if (e != null)
            {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(e.getMessage());
            }
            else if (result != null && result.containsKey("success") && !(Boolean)result.get("success"))
            {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(result.containsKey("message") ? (String)result.get("message") : "操作失败");
            }

            // 保存日志
            SpringUtils.getBean(ISysOperLogService.class).insertOperlog(operLog);
        }
        catch (Exception ex)
        {
            log.error("记录云函数操作日志异常", ex);
        }
    }
} 