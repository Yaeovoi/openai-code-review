package cn.Levionyx.middleware.sdk.domain.model;

/**
 * 支持的 AI 模型枚举
 */
public enum ChatModel {

    // 阿里云百炼 GLM 系列
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
        for (ChatModel model : values()) {
            if (model.code.equalsIgnoreCase(code)) {
                return model;
            }
        }
        return GLM_4_FLASH; // 默认使用 GLM-4-flash
    }
}