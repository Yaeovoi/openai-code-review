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

    public DeepSeek(String apiKey, int readTimeout) {
        super(DEFAULT_API_HOST, null, apiKey, DEFAULT_CONNECT_TIMEOUT, readTimeout);
    }

    public DeepSeek(String apiHost, String apiKey, int readTimeout) {
        super(DEFAULT_API_HOST, apiHost, apiKey, DEFAULT_CONNECT_TIMEOUT, readTimeout);
    }

    @Override
    protected String getApiName() {
        return "DeepSeek";
    }
}