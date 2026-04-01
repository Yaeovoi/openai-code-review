package cn.Levionyx.middleware.sdk.config;

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
     * 设置 AI 模型代码
     */
    public CodeReviewConfigBuilder model(String modelCode) {
        config.setChatModelCode(modelCode);
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
     * 设置 API 配置（自定义 Host 和协议）
     */
    public CodeReviewConfigBuilder api(String apiHost, String apiKey, String protocol) {
        config.setApiHost(apiHost);
        config.setApiKey(apiKey);
        config.setApiProtocol(protocol);
        return this;
    }

    /**
     * 设置 API 协议
     */
    public CodeReviewConfigBuilder protocol(String protocol) {
        config.setApiProtocol(protocol);
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
            if (config.getNotification().getFeishuAppId() == null || config.getNotification().getFeishuAppId().isEmpty()) {
                throw new IllegalArgumentException("飞书 AppId 不能为空");
            }
            if (config.getNotification().getFeishuAppSecret() == null || config.getNotification().getFeishuAppSecret().isEmpty()) {
                throw new IllegalArgumentException("飞书 AppSecret 不能为空");
            }
            if (config.getNotification().getFeishuChatId() == null || config.getNotification().getFeishuChatId().isEmpty()) {
                throw new IllegalArgumentException("飞书 ChatId 不能为空");
            }
        } else if ("dingtalk".equals(channel)) {
            if (config.getNotification().getDingtalkWebhook() == null || config.getNotification().getDingtalkWebhook().isEmpty()) {
                throw new IllegalArgumentException("钉钉 Webhook 不能为空");
            }
        } else if ("wecom".equals(channel)) {
            if (config.getNotification().getWecomWebhook() == null || config.getNotification().getWecomWebhook().isEmpty()) {
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
        config.setRepo(getEnvOrDefault("COMMIT_REPO", null));
        config.setBranch(getEnv("COMMIT_BRANCH"));
        config.setAuthor(getEnv("COMMIT_AUTHOR"));
        config.setMessage(getEnv("COMMIT_MESSAGE"));
        config.setCommitSha(getEnvOrDefault("COMMIT_SHA", null));

        // AI 模型配置
        // 用户配置了 CHAT_MODEL 就用用户的，否则用默认值
        String modelCode = getEnvOrDefault("CHAT_MODEL", "qwen-coder-plus");
        config.setChatModelCode(modelCode);
        config.setApiHost(getEnvOrDefault("API_HOST", null));
        config.setApiProtocol(getEnvOrDefault("API_PROTOCOL", "openai"));

        // 支持 API_KEY 或 GLM_API_KEY（向后兼容）
        String apiKey = getEnvOrDefault("API_KEY", null);
        if (apiKey == null) {
            apiKey = getEnvOrDefault("GLM_API_KEY", null);
        }
        if (apiKey == null) {
            throw new IllegalArgumentException("环境变量 API_KEY 或 GLM_API_KEY 未配置");
        }
        config.setApiKey(apiKey);

        // API 超时配置（单位：秒，默认 180 秒 = 3 分钟）
        String timeoutStr = getEnvOrDefault("API_TIMEOUT", "180");
        try {
            int timeoutSeconds = Integer.parseInt(timeoutStr);
            config.setApiTimeout(timeoutSeconds * 1000);  // 转换为毫秒
        } catch (NumberFormatException e) {
            config.setApiTimeout(180000);  // 默认 3 分钟
        }

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