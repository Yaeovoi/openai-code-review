package cn.Levionyx.middleware.sdk.domain.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 支持的 AI 模型枚举
 */
public enum ChatModel {

    // 阿里云百炼 Qwen 系列（codingplan 专用 API）
    QWEN_TURBO("qwen-turbo", "qwen", "阿里云百炼 Qwen-Turbo"),
    QWEN_PLUS("qwen-plus", "qwen", "阿里云百炼 Qwen-Plus"),
    QWEN_MAX("qwen-max", "qwen", "阿里云百炼 Qwen-Max"),
    QWEN_CODER_PLUS("qwen-coder-plus", "qwen", "阿里云百炼 Qwen-Coder-Plus（代码专用）"),

    // 阿里云百炼 GLM 系列（通过百炼代理）
    GLM_4_FLASH("glm-4-flash", "glm", "阿里云百炼 GLM-4-Flash"),
    GLM_4("glm-4", "glm", "阿里云百炼 GLM-4"),

    // OpenAI 系列
    GPT_4O("gpt-4o", "openai", "OpenAI GPT-4o"),
    GPT_4O_MINI("gpt-4o-mini", "openai", "OpenAI GPT-4o-mini"),
    GPT_4_TURBO("gpt-4-turbo", "openai", "OpenAI GPT-4-turbo"),

    // DeepSeek 系列
    DEEPSEEK_CHAT("deepseek-chat", "deepseek", "DeepSeek Chat"),
    DEEPSEEK_CODER("deepseek-coder", "deepseek", "DeepSeek Coder");

    private final String code;
    private final String provider;
    private final String description;

    ChatModel(String code, String provider, String description) {
        this.code = code;
        this.provider = provider;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getProvider() {
        return provider;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据模型代码查找枚举
     * 如果找不到，返回 null（由调用方决定如何处理）
     */
    public static ChatModel fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ChatModel model : values()) {
            if (model.code.equalsIgnoreCase(code)) {
                return model;
            }
        }
        return null;  // 找不到返回 null，不强制使用默认值
    }

    /**
     * 根据模型代码查找枚举，找不到时返回默认值
     * 仅在用户未配置 CHAT_MODEL 时使用
     */
    public static ChatModel fromCodeOrDefault(String code) {
        if (code == null || code.isEmpty()) {
            logger.info("未配置模型，使用默认模型: {}", QWEN_CODER_PLUS.getCode());
            return QWEN_CODER_PLUS;
        }
        ChatModel model = fromCode(code);
        if (model == null) {
            logger.info("模型 [{}] 不在预定义列表中，将使用用户配置的 API_HOST 或默认 OpenAI 协议", code);
        }
        return model;
    }

    /**
     * 根据模型代码推断 provider
     * 用于选择对应的实现类
     */
    public static String inferProvider(String modelCode) {
        if (modelCode == null || modelCode.isEmpty()) {
            return "qwen";  // 默认 provider
        }

        // 先在枚举中查找
        ChatModel model = fromCode(modelCode);
        if (model != null) {
            return model.getProvider();
        }

        // 根据模型名称前缀推断
        String lowerCode = modelCode.toLowerCase();
        if (lowerCode.startsWith("qwen")) {
            return "qwen";
        }
        if (lowerCode.startsWith("glm")) {
            return "glm";
        }
        if (lowerCode.startsWith("gpt") || lowerCode.startsWith("o1") || lowerCode.startsWith("o3")) {
            return "openai";
        }
        if (lowerCode.startsWith("deepseek")) {
            return "deepseek";
        }
        if (lowerCode.startsWith("claude")) {
            return "anthropic";
        }

        // 默认使用 openai 协议
        return "openai";
    }

    private static final Logger logger = LoggerFactory.getLogger(ChatModel.class);
}