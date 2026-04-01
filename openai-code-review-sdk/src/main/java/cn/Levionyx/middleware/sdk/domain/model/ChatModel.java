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
     */
    public static ChatModel fromCode(String code) {
        if (code == null || code.isEmpty()) {
            logger.warn("模型代码为空，使用默认模型: {}", QWEN_CODER_PLUS.getCode());
            return QWEN_CODER_PLUS;
        }
        for (ChatModel model : values()) {
            if (model.code.equalsIgnoreCase(code)) {
                return model;
            }
        }
        logger.warn("未找到模型代码 [{}]，使用默认模型: {}", code, QWEN_CODER_PLUS.getCode());
        return QWEN_CODER_PLUS;
    }

    private static final Logger logger = LoggerFactory.getLogger(ChatModel.class);
}