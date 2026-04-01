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
 *
 * 模型选择逻辑：
 * 1. 用户配置了 API_PROTOCOL=anthropic → 使用 Anthropic 实现
 * 2. 用户配置了 API_HOST → 根据模型名称推断 provider，使用自定义地址
 * 3. 模型在预定义枚举中 → 使用对应的默认实现和地址
 * 4. 模型不在枚举中 → 根据模型名称前缀推断 provider
 */
public class ChatModelFactory {

    // 默认协议
    private static final String DEFAULT_PROTOCOL = "openai";
    // 默认超时时间（毫秒）
    private static final int DEFAULT_TIMEOUT = 180000; // 3分钟

    /**
     * 根据模型代码创建 AI 模型实例
     *
     * @param modelCode 模型代码（用户配置的原始值，如 glm-5, qwen-coder-plus）
     * @param apiHost   自定义 API Host（可选）
     * @param apiKey    API Key
     * @param protocol  API 协议：openai 或 anthropic
     * @return IOpenAI 实例
     */
    public static IOpenAI create(String modelCode, String apiHost, String apiKey, String protocol) {
        return create(modelCode, apiHost, apiKey, protocol, DEFAULT_TIMEOUT);
    }

    /**
     * 根据模型代码创建 AI 模型实例（支持自定义超时）
     *
     * @param modelCode   模型代码
     * @param apiHost     自定义 API Host（可选）
     * @param apiKey      API Key
     * @param protocol    API 协议：openai 或 anthropic
     * @param readTimeout 读取超时时间（毫秒）
     * @return IOpenAI 实例
     */
    public static IOpenAI create(String modelCode, String apiHost, String apiKey, String protocol, int readTimeout) {
        // 1. 如果指定了 anthropic 协议，直接使用 Anthropic 实现
        if ("anthropic".equalsIgnoreCase(protocol)) {
            if (apiHost != null && !apiHost.isEmpty()) {
                return new Anthropic(apiHost, apiKey, readTimeout);
            }
            return new Anthropic(apiKey, readTimeout);
        }

        // 2. 获取枚举中的模型（可能为 null）
        ChatModel model = ChatModel.fromCode(modelCode);

        // 3. 确定 provider
        String provider;
        if (model != null) {
            provider = model.getProvider();
        } else {
            // 用户自定义模型，根据模型名称推断 provider
            provider = ChatModel.inferProvider(modelCode);
        }

        // 4. 用户配置了自定义 API_HOST，优先使用
        if (apiHost != null && !apiHost.isEmpty()) {
            return createByProvider(provider, apiHost, apiKey, readTimeout);
        }

        // 5. 使用默认 API 地址
        return createByProvider(provider, null, apiKey, readTimeout);
    }

    /**
     * 根据 provider 创建对应的实现实例
     */
    private static IOpenAI createByProvider(String provider, String apiHost, String apiKey, int readTimeout) {
        switch (provider) {
            case "qwen":
                return apiHost != null ? new Qwen(apiHost, apiKey, readTimeout) : new Qwen(apiKey, readTimeout);
            case "glm":
                return apiHost != null ? new GLM(apiHost, apiKey, readTimeout) : new GLM(apiKey, readTimeout);
            case "openai":
                return apiHost != null ? new OpenAI(apiHost, apiKey, readTimeout) : new OpenAI(apiKey, readTimeout);
            case "deepseek":
                return apiHost != null ? new DeepSeek(apiHost, apiKey, readTimeout) : new DeepSeek(apiKey, readTimeout);
            default:
                // 默认使用 OpenAI 协议（兼容大多数 API）
                return apiHost != null ? new OpenAI(apiHost, apiKey, readTimeout) : new OpenAI(apiKey, readTimeout);
        }
    }
}