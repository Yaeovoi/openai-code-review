package cn.Levionyx.middleware.sdk.infrastructure.openai.impl;

/**
 * DeepSeek 模型实现
 * API 文档: https://platform.deepseek.com/api-docs/
 * 使用 OpenAI 兼容接口
 */
public class DeepSeek extends AbstractOpenAI {

    private static final String DEFAULT_API_HOST = "https://api.deepseek.com/v1/chat/completions";

    public DeepSeek(String apiKey) {
        super(DEFAULT_API_HOST, apiKey);
    }

    public DeepSeek(String apiHost, String apiKey) {
        super(DEFAULT_API_HOST, apiHost, apiKey);
    }

    @Override
    protected String getApiName() {
        return "DeepSeek";
    }
}