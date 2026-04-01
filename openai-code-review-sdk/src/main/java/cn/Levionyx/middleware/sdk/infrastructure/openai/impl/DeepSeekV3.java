package cn.Levionyx.middleware.sdk.infrastructure.openai.impl;

/**
 * DeepSeek V3 模型实现
 * API 文档: https://platform.deepseek.com/api-docs/
 * 使用 OpenAI 兼容接口
 */
public class DeepSeekV3 extends AbstractOpenAI {

    private static final String DEFAULT_API_HOST = "https://api.deepseek.com/v1/chat/completions";

    public DeepSeekV3(String apiKey) {
        super(DEFAULT_API_HOST, apiKey);
    }

    public DeepSeekV3(String apiHost, String apiKey) {
        super(DEFAULT_API_HOST, apiHost, apiKey);
    }

    @Override
    protected String getApiName() {
        return "DeepSeekV3";
    }
}