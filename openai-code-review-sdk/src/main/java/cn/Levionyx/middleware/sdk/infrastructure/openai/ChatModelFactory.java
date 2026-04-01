package cn.Levionyx.middleware.sdk.infrastructure.openai;

import cn.Levionyx.middleware.sdk.domain.model.ChatModel;
import cn.Levionyx.middleware.sdk.infrastructure.openai.impl.Anthropic;
import cn.Levionyx.middleware.sdk.infrastructure.openai.impl.DeepSeek;
import cn.Levionyx.middleware.sdk.infrastructure.openai.impl.GLM;
import cn.Levionyx.middleware.sdk.infrastructure.openai.impl.OpenAI;
import cn.Levionyx.middleware.sdk.infrastructure.openai.impl.Qwen;

/**
 * AI 模型工厂 - 根据配置创建对应的模型实例
 *
 * 支持两种 API 协议：
 * - openai: OpenAI Chat Completions API 协议（大多数模型服务兼容）
 * - anthropic: Anthropic Claude API 协议
 */
public class ChatModelFactory {

    // 默认协议
    private static final String DEFAULT_PROTOCOL = "openai";

    /**
     * 创建 AI 模型实例
     *
     * @param model  模型枚举
     * @param apiKey API Key
     * @return IOpenAI 实例
     */
    public static IOpenAI create(ChatModel model, String apiKey) {
        return create(model, null, apiKey, DEFAULT_PROTOCOL);
    }

    /**
     * 创建 AI 模型实例（支持自定义 API Host）
     *
     * @param model   模型枚举
     * @param apiHost 自定义 API Host（可选）
     * @param apiKey  API Key
     * @return IOpenAI 实例
     */
    public static IOpenAI create(ChatModel model, String apiHost, String apiKey) {
        return create(model, apiHost, apiKey, DEFAULT_PROTOCOL);
    }

    /**
     * 创建 AI 模型实例（支持自定义 API Host 和协议）
     *
     * @param model     模型枚举
     * @param apiHost   自定义 API Host（可选）
     * @param apiKey    API Key
     * @param protocol  API 协议：openai 或 anthropic
     * @return IOpenAI 实例
     */
    public static IOpenAI create(ChatModel model, String apiHost, String apiKey, String protocol) {
        // 如果指定了 anthropic 协议，直接使用 Anthropic 实现
        if ("anthropic".equalsIgnoreCase(protocol)) {
            if (apiHost != null && !apiHost.isEmpty()) {
                return new Anthropic(apiHost, apiKey);
            }
            return new Anthropic(apiKey);
        }

        // 默认使用 OpenAI 协议，根据 provider 选择实现
        String provider = model.getProvider();

        switch (provider) {
            case "qwen":
                if (apiHost != null && !apiHost.isEmpty()) {
                    return new Qwen(apiHost, apiKey);
                }
                return new Qwen(apiKey);

            case "glm":
                if (apiHost != null && !apiHost.isEmpty()) {
                    return new GLM(apiHost, apiKey);
                }
                return new GLM(apiKey);

            case "openai":
                if (apiHost != null && !apiHost.isEmpty()) {
                    return new OpenAI(apiHost, apiKey);
                }
                return new OpenAI(apiKey);

            case "deepseek":
                if (apiHost != null && !apiHost.isEmpty()) {
                    return new DeepSeek(apiHost, apiKey);
                }
                return new DeepSeek(apiKey);

            default:
                throw new IllegalArgumentException("不支持的模型提供商: " + provider);
        }
    }

    /**
     * 根据模型代码创建 AI 模型实例
     *
     * @param modelCode 模型代码（如 glm-4-flash, gpt-4o）
     * @param apiKey    API Key
     * @return IOpenAI 实例
     */
    public static IOpenAI createByCode(String modelCode, String apiKey) {
        ChatModel model = ChatModel.fromCode(modelCode);
        return create(model, apiKey);
    }

    /**
     * 根据模型代码创建 AI 模型实例（支持自定义 API Host）
     *
     * @param modelCode 模型代码
     * @param apiHost   自定义 API Host
     * @param apiKey    API Key
     * @return IOpenAI 实例
     */
    public static IOpenAI createByCode(String modelCode, String apiHost, String apiKey) {
        ChatModel model = ChatModel.fromCode(modelCode);
        return create(model, apiHost, apiKey);
    }

    /**
     * 根据模型代码创建 AI 模型实例（支持自定义 API Host 和协议）
     *
     * @param modelCode 模型代码
     * @param apiHost   自定义 API Host
     * @param apiKey    API Key
     * @param protocol  API 协议：openai 或 anthropic
     * @return IOpenAI 实例
     */
    public static IOpenAI createByCode(String modelCode, String apiHost, String apiKey, String protocol) {
        ChatModel model = ChatModel.fromCode(modelCode);
        return create(model, apiHost, apiKey, protocol);
    }
}