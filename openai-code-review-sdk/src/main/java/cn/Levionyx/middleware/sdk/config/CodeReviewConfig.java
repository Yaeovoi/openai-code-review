package cn.Levionyx.middleware.sdk.config;

/**
 * 代码审查配置类
 */
public class CodeReviewConfig {

    // ========== Git 配置 ==========
    private String githubReviewLogUri;
    private String githubToken;
    private String project;
    private String branch;
    private String author;
    private String message;

    // ========== AI 模型配置 ==========
    private String chatModelCode;  // 用户配置的原始模型代码
    private String apiHost;  // 可选，自定义 API 地址
    private String apiProtocol;  // 可选，API 协议：openai 或 anthropic
    private String apiKey;

    // ========== 通知配置 ==========
    private NotificationConfig notification = new NotificationConfig();

    // ========== 审查规则配置 ==========
    private ReviewRules reviewRules = new ReviewRules();

    // ========== 通知配置内部类 ==========
    public static class NotificationConfig {
        private String channel = "feishu";  // feishu, dingtalk, wecom

        // 飞书配置
        private String feishuAppId;
        private String feishuAppSecret;
        private String feishuChatId;

        // 钉钉配置
        private String dingtalkWebhook;

        // 企业微信配置
        private String wecomWebhook;

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getFeishuAppId() {
            return feishuAppId;
        }

        public void setFeishuAppId(String feishuAppId) {
            this.feishuAppId = feishuAppId;
        }

        public String getFeishuAppSecret() {
            return feishuAppSecret;
        }

        public void setFeishuAppSecret(String feishuAppSecret) {
            this.feishuAppSecret = feishuAppSecret;
        }

        public String getFeishuChatId() {
            return feishuChatId;
        }

        public void setFeishuChatId(String feishuChatId) {
            this.feishuChatId = feishuChatId;
        }

        public String getDingtalkWebhook() {
            return dingtalkWebhook;
        }

        public void setDingtalkWebhook(String dingtalkWebhook) {
            this.dingtalkWebhook = dingtalkWebhook;
        }

        public String getWecomWebhook() {
            return wecomWebhook;
        }

        public void setWecomWebhook(String wecomWebhook) {
            this.wecomWebhook = wecomWebhook;
        }
    }

    // ========== 审查规则配置内部类 ==========
    public static class ReviewRules {
        private boolean checkCodeQuality = true;
        private boolean checkSecurity = true;
        private boolean checkPerformance = true;
        private boolean checkBestPractices = true;
        private String customPrompt;  // 自定义提示词

        public boolean isCheckCodeQuality() {
            return checkCodeQuality;
        }

        public void setCheckCodeQuality(boolean checkCodeQuality) {
            this.checkCodeQuality = checkCodeQuality;
        }

        public boolean isCheckSecurity() {
            return checkSecurity;
        }

        public void setCheckSecurity(boolean checkSecurity) {
            this.checkSecurity = checkSecurity;
        }

        public boolean isCheckPerformance() {
            return checkPerformance;
        }

        public void setCheckPerformance(boolean checkPerformance) {
            this.checkPerformance = checkPerformance;
        }

        public boolean isCheckBestPractices() {
            return checkBestPractices;
        }

        public void setCheckBestPractices(boolean checkBestPractices) {
            this.checkBestPractices = checkBestPractices;
        }

        public String getCustomPrompt() {
            return customPrompt;
        }

        public void setCustomPrompt(String customPrompt) {
            this.customPrompt = customPrompt;
        }

        /**
         * 生成系统提示词
         */
        public String generateSystemPrompt() {
            if (customPrompt != null && !customPrompt.isEmpty()) {
                return customPrompt;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("你是一位专业的代码评审专家。请对提交的代码进行审查，关注以下方面：\n");

            if (checkCodeQuality) {
                sb.append("1. 代码质量和可读性\n");
            }
            if (checkSecurity) {
                sb.append("2. 潜在的安全隐患\n");
            }
            if (checkPerformance) {
                sb.append("3. 性能问题\n");
            }
            if (checkBestPractices) {
                sb.append("4. 最佳实践建议\n");
            }

            sb.append("请用中文给出简洁、专业的评审意见。");
            return sb.toString();
        }
    }

    // ========== Getters and Setters ==========

    public String getGithubReviewLogUri() {
        return githubReviewLogUri;
    }

    public void setGithubReviewLogUri(String githubReviewLogUri) {
        this.githubReviewLogUri = githubReviewLogUri;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getChatModelCode() {
        return chatModelCode;
    }

    public void setChatModelCode(String chatModelCode) {
        this.chatModelCode = chatModelCode;
    }

    public String getApiHost() {
        return apiHost;
    }

    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }

    public String getApiProtocol() {
        return apiProtocol;
    }

    public void setApiProtocol(String apiProtocol) {
        this.apiProtocol = apiProtocol;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public NotificationConfig getNotification() {
        return notification;
    }

    public void setNotification(NotificationConfig notification) {
        this.notification = notification;
    }

    public ReviewRules getReviewRules() {
        return reviewRules;
    }

    public void setReviewRules(ReviewRules reviewRules) {
        this.reviewRules = reviewRules;
    }
}