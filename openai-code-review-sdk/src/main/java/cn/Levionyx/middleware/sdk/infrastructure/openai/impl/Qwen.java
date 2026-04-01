package cn.Levionyx.middleware.sdk.infrastructure.openai.impl;

/**
 * 阿里云百炼 Qwen 模型实现
 * API 文档: https://help.aliyun.com/zh/model-studio/developer-reference/use-qwen-by-calling-api
 *
 * 支持两种 API 地址：
 * 1. 百炼 OpenAI 兼容 API（默认）: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
 * 2. 灵码 codingplan API: https://coding.dashscope.aliyuncs.com/v1/chat/completions
 */
public class Qwen extends AbstractOpenAI {

    private static final String DEFAULT_API_HOST = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    public Qwen(String apiKey) {
        super(DEFAULT_API_HOST, apiKey);
    }

    public Qwen(String apiHost, String apiKey) {
        super(DEFAULT_API_HOST, apiHost, apiKey);
    }

    @Override
    protected String getApiName() {
        return "Qwen";
    }
}