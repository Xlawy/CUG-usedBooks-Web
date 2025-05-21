package com.ruoyi.web.controller.system;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.IAIService;

/**
 * AI聊天服务控制器
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/ai")
public class SysAIController extends BaseController
{
    @Autowired
    private IAIService aiService;

    /**
     * 获取AI配置
     */
    @GetMapping("/config")
    public AjaxResult getConfig()
    {
        return success(aiService.getAIConfig());
    }

    /**
     * 更新AI配置
     */
    @Log(title = "AI配置", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public AjaxResult updateConfig(@RequestBody Map<String, Object> config)
    {
        if (aiService.updateAIConfig(config))
        {
            return success();
        }
        return error("修改AI配置失败");
    }

    /**
     * 发送消息到AI服务
     */
    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody Map<String, Object> params)
    {
        String message = (String) params.get("message");
        if (StringUtils.isEmpty(message))
        {
            return error("消息内容不能为空");
        }

        try
        {
            Map<String, Object> result = aiService.sendMessage(params);
            
            // 详细日志记录响应内容
            logger.info("AI服务返回结果: {}", JSON.toJSONString(result));
            
            // 校验响应内容是否完整
            if (result != null) {
                logger.info("返回内容: {}", result.get("content"));
                logger.info("返回模型: {}", result.get("model"));
                logger.info("响应时间: {}", result.get("responseTime"));
            } else {
                logger.warn("AI服务返回结果为空");
            }
            
            // 构造返回数据 - 使用AjaxResult.success并传入data
            return AjaxResult.success("操作成功", result);
        }
        catch (Exception e)
        {
            logger.error("AI服务调用失败", e);
            return error("AI服务调用失败: " + e.getMessage());
        }
    }
}
