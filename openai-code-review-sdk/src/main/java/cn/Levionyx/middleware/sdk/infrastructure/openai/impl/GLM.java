package cn.Levionyx.middleware.sdk.infrastructure.openai.impl;

/**
 * 阿里云百炼 GLM 模型实现
 * API 文档: https://help.aliyun.com/zh/model-studio/developer-reference/use-qwen-by-calling-api
 *
 * 注意：智谱 GLM 模型通过阿里云百炼平台代理调用
 */
public class GLM extends AbstractOpenAI {

    private static final String DEFAULT_API_HOST = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    public GLM(String apiKey) {
        super(DEFAULT_API_HOST, apiKey);
    }

    public GLM(String apiHost, String apiKey) {
        super(DEFAULT_API_HOST, apiHost, apiKey);
    }

    public GLM(String apiKey, int readTimeout) {
        super(DEFAULT_API_HOST, null, apiKey, DEFAULT_CONNECT_TIMEOUT, readTimeout);
    }

    public GLM(String apiHost, String apiKey, int readTimeout) {
        super(DEFAULT_API_HOST, apiHost, apiKey, DEFAULT_CONNECT_TIMEOUT, readTimeout);
    }

    @Override
    protected String getApiName() {
        return "GLM";
    }
}