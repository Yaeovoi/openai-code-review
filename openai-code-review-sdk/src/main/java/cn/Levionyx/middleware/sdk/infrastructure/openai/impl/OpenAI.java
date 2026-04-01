package cn.Levionyx.middleware.sdk.infrastructure.openai.impl;

/**
 * OpenAI 模型实现
 * API 文档: https://platform.openai.com/docs/api-reference/chat
 */
public class OpenAI extends AbstractOpenAI {

    private static final String DEFAULT_API_HOST = "https://api.openai.com/v1/chat/completions";

    public OpenAI(String apiKey) {
        super(DEFAULT_API_HOST, apiKey);
    }

    public OpenAI(String apiHost, String apiKey) {
        super(DEFAULT_API_HOST, apiHost, apiKey);
    }

    public OpenAI(String apiKey, int readTimeout) {
        super(DEFAULT_API_HOST, null, apiKey, DEFAULT_CONNECT_TIMEOUT, readTimeout);
    }

    public OpenAI(String apiHost, String apiKey, int readTimeout) {
        super(DEFAULT_API_HOST, apiHost, apiKey, DEFAULT_CONNECT_TIMEOUT, readTimeout);
    }

    @Override
    protected String getApiName() {
        return "OpenAI";
    }
}