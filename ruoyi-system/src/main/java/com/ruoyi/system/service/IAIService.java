package com.ruoyi.system.service;

import java.util.Map;

/**
 * AI服务接口
 * 
 * @author ruoyi
 */
public interface IAIService
{
    /**
     * 获取AI配置
     * 
     * @return AI配置信息
     */
    public Map<String, Object> getAIConfig();

    /**
     * 更新AI配置
     * 
     * @param config 配置信息
     * @return 是否成功
     */
    public boolean updateAIConfig(Map<String, Object> config);

    /**
     * 发送消息到AI服务
     * 
     * @param params 包含message和可选的history等参数
     * @return AI响应结果
     */
    public Map<String, Object> sendMessage(Map<String, Object> params);
} 