package cn.Levionyx.middleware.sdk.infrastructure.openai;

import cn.Levionyx.middleware.sdk.domain.model.ChatModel;
import cn.Levionyx.middleware.sdk.infrastructure.openai.impl.DeepSeek;
import cn.Levionyx.middleware.sdk.infrastructure.openai.impl.GLM;
import cn.Levionyx.middleware.sdk.infrastructure.openai.impl.OpenAI;

/**
 * AI 模型工厂 - 根据配置创建对应的模型实例
 */
public class ChatModelFactory {

    /**
     * 创建 AI 模型实例
     *
     * @param model  模型枚举
     * @param apiKey API Key
     * @return IOpenAI 实例
     */
    public static IOpenAI create(ChatModel model, String apiKey) {
        return create(model, null, apiKey);
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
        String provider = model.getProvider();

        switch (provider) {
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
}