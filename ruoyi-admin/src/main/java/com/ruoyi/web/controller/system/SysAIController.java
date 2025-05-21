package com.ruoyi.web.controller.system;

import java.util.Map;
import java.util.HashMap;
import com.alibaba.fastjson2.JSONObject;

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
import com.ruoyi.system.service.IIntentProcessorService;

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

    @Autowired
    private IIntentProcessorService intentProcessorService;

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
            // 1. 调用AI服务识别意图
            Map<String, Object> aiResult = aiService.sendMessage(params);
            
            // 详细日志记录响应内容
            logger.info("AI服务返回结果: {}", JSON.toJSONString(aiResult));
            
            // 校验响应内容是否完整
            if (aiResult == null || !aiResult.containsKey("content")) {
                logger.warn("AI服务返回结果为空或缺少content字段");
                return error("无法获取有效的AI回复");
            }
            
            String content = (String) aiResult.get("content");
            logger.info("返回内容: {}", content);
            logger.info("返回模型: {}", aiResult.get("model"));
            logger.info("响应时间: {}", aiResult.get("responseTime"));
            
            // 2. 解析AI返回的JSON格式意图
            try {
                JSONObject intentData = JSON.parseObject(content);
                String intent = intentData.getString("intent");
                JSONObject parameters = intentData.getJSONObject("parameters");
                
                logger.info("识别到意图: {}, 参数: {}", intent, JSON.toJSONString(parameters));
                
                // 3. 调用意图处理服务执行实际查询
                if (intent != null && !"unknown".equals(intent)) {
                    Map<String, Object> intentResult = intentProcessorService.processIntent(
                        intent, 
                        parameters == null ? new HashMap<>() : parameters.toJavaObject(Map.class)
                    );
                    
                    logger.info("意图处理结果: {}", JSON.toJSONString(intentResult));
                    
                    // 4. 将AI结果和查询结果合并返回
                    Map<String, Object> finalResult = new HashMap<>();
                    // 保留原有AI识别结果
                    finalResult.put("aiResult", aiResult);
                    // 添加查询结果
                    finalResult.put("intentResult", intentResult);
                    finalResult.put("success", intentResult.containsKey("success") ? intentResult.get("success") : true);
                    
                    return AjaxResult.success("意图处理成功", finalResult);
                } else {
                    // 如果是未知意图，只返回AI结果
                    return AjaxResult.success("无法识别意图", aiResult);
                }
            } catch (Exception e) {
                logger.error("解析意图数据失败", e);
                // 解析失败，返回原始AI结果
                return AjaxResult.success("解析意图失败", aiResult);
            }
        }
        catch (Exception e)
        {
            logger.error("AI服务调用失败", e);
            return error("AI服务调用失败: " + e.getMessage());
        }
    }
}
