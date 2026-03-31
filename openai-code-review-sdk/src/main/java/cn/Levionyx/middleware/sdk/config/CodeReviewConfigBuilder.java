package cn.Levionyx.middleware.sdk.config;

import cn.Levionyx.middleware.sdk.domain.model.ChatModel;

/**
 * 配置构建器 - 流式 API 构建配置
 */
public class CodeReviewConfigBuilder {

    private final CodeReviewConfig config = new CodeReviewConfig();

    /**
     * 设置 GitHub 配置
     */
    public CodeReviewConfigBuilder github(String reviewLogUri, String token) {
        config.setGithubReviewLogUri(reviewLogUri);
        config.setGithubToken(token);
        return this;
    }

    /**
     * 设置提交信息
     */
    public CodeReviewConfigBuilder commit(String project, String branch, String author, String message) {
        config.setProject(project);
        config.setBranch(branch);
        config.setAuthor(author);
        config.setMessage(message);
        return this;
    }

    /**
     * 设置 AI 模型
     */
    public CodeReviewConfigBuilder model(ChatModel model) {
        config.setChatModel(model);
        return this;
    }

    /**
     * 设置 AI 模型（通过代码）
     */
    public CodeReviewConfigBuilder model(String modelCode) {
        config.setChatModel(ChatModel.fromCode(modelCode));
        return this;
    }

    /**
     * 设置 API 配置
     */
    public CodeReviewConfigBuilder api(String apiKey) {
        config.setApiKey(apiKey);
        return this;
    }

    /**
     * 设置 API 配置（自定义 Host）
     */
    public CodeReviewConfigBuilder api(String apiHost, String apiKey) {
        config.setApiHost(apiHost);
        config.setApiKey(apiKey);
        return this;
    }

    /**
     * 设置飞书通知
     */
    public CodeReviewConfigBuilder feishu(String appId, String appSecret, String chatId) {
        config.getNotification().setChannel("feishu");
        config.getNotification().setFeishuAppId(appId);
        config.getNotification().setFeishuAppSecret(appSecret);
        config.getNotification().setFeishuChatId(chatId);
        return this;
    }

    /**
     * 设置钉钉通知
     */
    public CodeReviewConfigBuilder dingtalk(String webhook) {
        config.getNotification().setChannel("dingtalk");
        config.getNotification().setDingtalkWebhook(webhook);
        return this;
    }

    /**
     * 设置企业微信通知
     */
    public CodeReviewConfigBuilder wecom(String webhook) {
        config.getNotification().setChannel("wecom");
        config.getNotification().setWecomWebhook(webhook);
        return this;
    }

    /**
     * 设置自定义提示词
     */
    public CodeReviewConfigBuilder customPrompt(String prompt) {
        config.getReviewRules().setCustomPrompt(prompt);
        return this;
    }

    /**
     * 设置审查规则
     */
    public CodeReviewConfigBuilder reviewRules(boolean codeQuality, boolean security, boolean performance, boolean bestPractices) {
        config.getReviewRules().setCheckCodeQuality(codeQuality);
        config.getReviewRules().setCheckSecurity(security);
        config.getReviewRules().setCheckPerformance(performance);
        config.getReviewRules().setCheckBestPractices(bestPractices);
        return this;
    }

    /**
     * 构建配置
     */
    public CodeReviewConfig build() {
        validate();
        return config;
    }

    private void validate() {
        if (config.getGithubReviewLogUri() == null || config.getGithubReviewLogUri().isEmpty()) {
            throw new IllegalArgumentException("GitHub 审查日志 URI 不能为空");
        }
        if (config.getGithubToken() == null || config.getGithubToken().isEmpty()) {
            throw new IllegalArgumentException("GitHub Token 不能为空");
        }
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }

        String channel = config.getNotification().getChannel();
        if ("feishu".equals(channel)) {
            if (config.getNotification().getFeishuAppId() == null || config.getNotification().getFeishuAppSecret() == null) {
                throw new IllegalArgumentException("飞书 AppId 和 AppSecret 不能为空");
            }
        } else if ("dingtalk".equals(channel)) {
            if (config.getNotification().getDingtalkWebhook() == null) {
                throw new IllegalArgumentException("钉钉 Webhook 不能为空");
            }
        } else if ("wecom".equals(channel)) {
            if (config.getNotification().getWecomWebhook() == null) {
                throw new IllegalArgumentException("企业微信 Webhook 不能为空");
            }
        }
    }

    /**
     * 从环境变量创建配置
     */
    public static CodeReviewConfig fromEnv() {
        CodeReviewConfig config = new CodeReviewConfig();

        // GitHub 配置
        config.setGithubReviewLogUri(getEnv("GITHUB_REVIEW_LOG_URI"));
        config.setGithubToken(getEnv("GITHUB_TOKEN"));
        config.setProject(getEnv("COMMIT_PROJECT"));
        config.setBranch(getEnv("COMMIT_BRANCH"));
        config.setAuthor(getEnv("COMMIT_AUTHOR"));
        config.setMessage(getEnv("COMMIT_MESSAGE"));

        // AI 模型配置
        String modelCode = getEnvOrDefault("CHAT_MODEL", "glm-4-flash");
        config.setChatModel(ChatModel.fromCode(modelCode));
        config.setApiHost(getEnvOrDefault("API_HOST", null));
        config.setApiKey(getEnv("API_KEY"));

        // 通知配置
        String channel = getEnvOrDefault("NOTIFICATION_CHANNEL", "feishu");
        config.getNotification().setChannel(channel);

        if ("feishu".equals(channel)) {
            config.getNotification().setFeishuAppId(getEnv("FEISHU_APP_ID"));
            config.getNotification().setFeishuAppSecret(getEnv("FEISHU_APP_SECRET"));
            config.getNotification().setFeishuChatId(getEnv("FEISHU_CHAT_ID"));
        } else if ("dingtalk".equals(channel)) {
            config.getNotification().setDingtalkWebhook(getEnv("DINGTALK_WEBHOOK"));
        } else if ("wecom".equals(channel)) {
            config.getNotification().setWecomWebhook(getEnv("WECOM_WEBHOOK"));
        }

        // 自定义提示词
        config.getReviewRules().setCustomPrompt(getEnvOrDefault("CUSTOM_PROMPT", null));

        return config;
    }

    private static String getEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("环境变量 " + key + " 未配置");
        }
        return value;
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}