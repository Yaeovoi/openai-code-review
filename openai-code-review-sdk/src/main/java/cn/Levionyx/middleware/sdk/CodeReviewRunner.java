package cn.Levionyx.middleware.sdk;

import cn.Levionyx.middleware.sdk.config.CodeReviewConfig;
import cn.Levionyx.middleware.sdk.config.CodeReviewConfigBuilder;
import cn.Levionyx.middleware.sdk.domain.model.review.CodeReviewResult;
import cn.Levionyx.middleware.sdk.domain.model.review.ReviewIssue;
import cn.Levionyx.middleware.sdk.domain.model.review.ReviewMetrics;
import cn.Levionyx.middleware.sdk.infrastructure.git.GitCommand;
import cn.Levionyx.middleware.sdk.infrastructure.github.GitHubPrComment;
import cn.Levionyx.middleware.sdk.infrastructure.notification.INotification;
import cn.Levionyx.middleware.sdk.infrastructure.notification.NotificationContentFormatter;
import cn.Levionyx.middleware.sdk.infrastructure.notification.impl.DingTalkNotification;
import cn.Levionyx.middleware.sdk.infrastructure.notification.impl.FeiShuNotification;
import cn.Levionyx.middleware.sdk.infrastructure.notification.impl.WeComNotification;
import cn.Levionyx.middleware.sdk.infrastructure.openai.ChatModelFactory;
import cn.Levionyx.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码审查运行器 - 支持配置化的主入口
 */
public class CodeReviewRunner {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewRunner.class);

    private final CodeReviewConfig config;
    private final GitCommand gitCommand;
    private final IOpenAI chatModel;
    private final INotification notification;

    public CodeReviewRunner(CodeReviewConfig config) {
        this.config = config;
        this.gitCommand = createGitCommand();
        this.chatModel = createChatModel();
        this.notification = createNotification();
    }

    /**
     * 执行代码审查
     */
    public void run() {
        try {
            logger.info("开始代码审查...");

            // 1. 获取代码差异
            String diffCode = gitCommand.diff();
            logger.info("获取到代码差异, 长度: {}", diffCode.length());

            // 2. AI 审查（返回结构化结果）
            CodeReviewResult result = review(diffCode);
            logger.info("代码审查完成，质量评分: {}, 问题数: {}",
                    result.getQualityScore(),
                    result.getSummary().getTotalIssues());

            // 3. 发布 PR 评论（如果是 PR 事件）
            if (shouldPostPrComment()) {
                postPrComment(result, diffCode);
            }

            // 4. 保存审查结果
            String reviewMarkdown = formatResultAsMarkdown(result);
            String logUrl = gitCommand.commitAndPush(reviewMarkdown);
            logger.info("审查结果已保存: {}", logUrl);

            // 5. 发送通知
            String commitUrl = buildCommitUrl(config.getRepo(), config.getCommitSha());
            String shortSummary = NotificationContentFormatter.formatReviewSummary(result);
            notification.send(logUrl, config.getProject(), config.getBranch(), config.getAuthor(), config.getMessage(), commitUrl, shortSummary);
            logger.info("代码审查通知已发送");

        } catch (Exception e) {
            logger.error("代码审查失败", e);
            throw new RuntimeException("代码审查失败", e);
        }
    }

    /**
     * 使用 AI 模型进行代码审查（返回结构化结果）
     */
    private CodeReviewResult review(String diffCode) throws Exception {
        ChatCompletionRequestDTO request = new ChatCompletionRequestDTO();
        request.setModel(config.getChatModelCode());

        List<ChatCompletionRequestDTO.Prompt> messages = new ArrayList<>();

        // 系统提示 - 要求返回 JSON 格式
        ChatCompletionRequestDTO.Prompt systemPrompt = new ChatCompletionRequestDTO.Prompt();
        systemPrompt.setRole("system");
        systemPrompt.setContent(buildSystemPrompt());
        messages.add(systemPrompt);

        // 用户提示
        ChatCompletionRequestDTO.Prompt userPrompt = new ChatCompletionRequestDTO.Prompt();
        userPrompt.setRole("user");
        userPrompt.setContent("请审查以下代码变更并返回 JSON 格式结果：\n\n" + diffCode);
        messages.add(userPrompt);

        request.setMessages(messages);

        ChatCompletionSyncResponseDTO response = chatModel.completions(request);

        // 解析响应
        String content = extractContent(response);
        if (content == null || content.isEmpty()) {
            logger.warn("AI 模型返回空响应");
            return createDefaultResult();
        }

        return parseReviewResult(content);
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt() {
        return "你是一位专业的代码评审专家。请审查代码并返回 JSON 格式结果。\n\n" +
            "【输出要求】\n" +
            "1. 必须只输出纯 JSON，不要包含 markdown 代码块标记\n" +
            "2. 所有文本内容使用中文\n\n" +
            "【问题类型】\n" +
            "- security: 安全漏洞（SQL注入、XSS、命令注入、硬编码密钥等）\n" +
            "- bug: 潜在 Bug 和逻辑错误\n" +
            "- performance: 性能问题\n" +
            "- style: 代码风格问题\n" +
            "- maintainability: 可维护性问题\n\n" +
            "【严重程度】\n" +
            "- critical: 严重（必须立即修复）\n" +
            "- high: 高危（强烈建议修复）\n" +
            "- medium: 中危（建议修复）\n" +
            "- low: 低危（可选优化）\n\n" +
            "【JSON Schema】\n" +
            "{\n" +
            "  \"qualityScore\": 85,\n" +
            "  \"summary\": {\n" +
            "    \"totalIssues\": 5,\n" +
            "    \"criticalIssues\": 0,\n" +
            "    \"highIssues\": 1,\n" +
            "    \"mediumIssues\": 2,\n" +
            "    \"lowIssues\": 2\n" +
            "  },\n" +
            "  \"issues\": [\n" +
            "    {\n" +
            "      \"type\": \"security\",\n" +
            "      \"severity\": \"high\",\n" +
            "      \"title\": \"SQL注入风险\",\n" +
            "      \"description\": \"用户输入直接拼接到SQL语句\",\n" +
            "      \"suggestion\": \"使用参数化查询或ORM框架\",\n" +
            "      \"file\": \"UserService.java\",\n" +
            "      \"line\": 45,\n" +
            "      \"codeSnippet\": \"String sql = \\\"SELECT * FROM users WHERE id = \\\" + userId;\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metrics\": {\n" +
            "    \"security\": 90,\n" +
            "    \"performance\": 85,\n" +
            "    \"maintainability\": 80,\n" +
            "    \"style\": 88\n" +
            "  }\n" +
            "}\n\n" +
            "【重要提示】\n" +
            "- qualityScore: 整体代码质量评分，0-100\n" +
            "- file: 从 diff 中提取文件路径（diff --git a/xxx b/yyy 中的 yyy）\n" +
            "- line: 变更行号，从 diff 的 @@ -x,y +a,b @@ 中推算\n" +
            "- 如果无法确定行号，line 填 null\n" +
            "- 只报告真正重要的问题，不要过度报告";
    }

    /**
     * 提取响应内容
     */
    private String extractContent(ChatCompletionSyncResponseDTO response) {
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatCompletionSyncResponseDTO.Choice choice = response.getChoices().get(0);
            if (choice != null && choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
        }
        return null;
    }

    /**
     * 解析审查结果
     */
    private CodeReviewResult parseReviewResult(String content) {
        CodeReviewResult result = new CodeReviewResult();

        try {
            // 清理 markdown 标记
            String jsonStr = content.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            } else if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            // 解析 JSON
            JSONObject json = JSON.parseObject(jsonStr);

            // 解析质量评分
            result.setQualityScore(json.getIntValue("qualityScore", 80));

            // 解析汇总
            JSONObject summaryJson = json.getJSONObject("summary");
            if (summaryJson != null) {
                CodeReviewResult.Summary summary = result.getSummary();
                summary.setTotalIssues(summaryJson.getIntValue("totalIssues", 0));
                summary.setCriticalIssues(summaryJson.getIntValue("criticalIssues", 0));
                summary.setHighIssues(summaryJson.getIntValue("highIssues", 0));
                summary.setMediumIssues(summaryJson.getIntValue("mediumIssues", 0));
                summary.setLowIssues(summaryJson.getIntValue("lowIssues", 0));
            }

            // 解析维度评分
            JSONObject metricsJson = json.getJSONObject("metrics");
            if (metricsJson != null) {
                ReviewMetrics metrics = result.getMetrics();
                metrics.setSecurity(metricsJson.getIntValue("security", 80));
                metrics.setPerformance(metricsJson.getIntValue("performance", 80));
                metrics.setMaintainability(metricsJson.getIntValue("maintainability", 80));
                metrics.setStyle(metricsJson.getIntValue("style", 80));
            }

            // 解析问题列表
            JSONArray issuesJson = json.getJSONArray("issues");
            if (issuesJson != null) {
                for (int i = 0; i < issuesJson.size(); i++) {
                    JSONObject issueJson = issuesJson.getJSONObject(i);
                    ReviewIssue issue = new ReviewIssue();
                    issue.setType(issueJson.getString("type"));
                    issue.setSeverity(issueJson.getString("severity"));
                    issue.setTitle(issueJson.getString("title"));
                    issue.setDescription(issueJson.getString("description"));
                    issue.setSuggestion(issueJson.getString("suggestion"));
                    issue.setFile(issueJson.getString("file"));
                    issue.setLine(issueJson.getInteger("line"));
                    issue.setCodeSnippet(issueJson.getString("codeSnippet"));
                    result.addIssue(issue);
                }
            }

            result.setRawContent(content);

        } catch (Exception e) {
            logger.error("解析审查结果失败: {}", e.getMessage());
            result.setQualityScore(75);
            result.setRawContent(content);
        }

        return result;
    }

    /**
     * 创建默认结果
     */
    private CodeReviewResult createDefaultResult() {
        CodeReviewResult result = new CodeReviewResult();
        result.setQualityScore(80);
        result.setRawContent("代码审查完成，未发现明显问题。");
        return result;
    }

    /**
     * 判断是否应该发布 PR 评论
     */
    private boolean shouldPostPrComment() {
        String prNumber = System.getenv("GITHUB_PR_NUMBER");
        return prNumber != null && !prNumber.isEmpty();
    }

    /**
     * 发布 PR 评论
     */
    private void postPrComment(CodeReviewResult result, String diffCode) {
        try {
            String githubToken = System.getenv("GITHUB_TOKEN");
            String repo = config.getRepo();
            String prNumberStr = System.getenv("GITHUB_PR_NUMBER");
            String sha = config.getCommitSha();

            if (githubToken == null || repo == null || prNumberStr == null) {
                logger.warn("缺少必要的环境变量，跳过 PR 评论");
                return;
            }

            int prNumber = Integer.parseInt(prNumberStr);
            GitHubPrComment prComment = new GitHubPrComment(githubToken, repo, prNumber, sha);
            prComment.postReviewComment(result, diffCode);

            logger.info("PR 评论发布成功");
        } catch (Exception e) {
            logger.error("发布 PR 评论失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 格式化为 Markdown 报告
     */
    private String formatResultAsMarkdown(CodeReviewResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 代码审查报告\n\n");

        // 质量评分
        sb.append("## 📊 质量评分\n\n");
        sb.append("**").append(result.getQualityScore()).append("/100**\n\n");

        // 统计信息
        CodeReviewResult.Summary summary = result.getSummary();
        sb.append("## 📈 问题统计\n\n");
        sb.append("| 严重程度 | 数量 |\n");
        sb.append("|----------|------|\n");
        sb.append("| 🔴 严重 | ").append(summary.getCriticalIssues()).append(" |\n");
        sb.append("| 🟠 高危 | ").append(summary.getHighIssues()).append(" |\n");
        sb.append("| 🟡 中危 | ").append(summary.getMediumIssues()).append(" |\n");
        sb.append("| 🟢 低危 | ").append(summary.getLowIssues()).append(" |\n");
        sb.append("| **总计** | **").append(summary.getTotalIssues()).append("** |\n\n");

        // 维度评分
        if (result.getMetrics() != null) {
            sb.append("## 📐 维度评分\n\n");
            sb.append("| 维度 | 评分 |\n");
            sb.append("|------|------|\n");
            sb.append("| 🔒 安全性 | ").append(result.getMetrics().getSecurity()).append("/100 |\n");
            sb.append("| ⚡ 性能 | ").append(result.getMetrics().getPerformance()).append("/100 |\n");
            sb.append("| 🔧 可维护性 | ").append(result.getMetrics().getMaintainability()).append("/100 |\n");
            sb.append("| 🎨 代码风格 | ").append(result.getMetrics().getStyle()).append("/100 |\n\n");
        }

        // 问题列表
        if (result.getIssues() != null && !result.getIssues().isEmpty()) {
            sb.append("## 📋 问题列表\n\n");

            for (int i = 0; i < result.getIssues().size(); i++) {
                ReviewIssue issue = result.getIssues().get(i);
                sb.append("### ").append(i + 1).append(". ")
                        .append(issue.getSeverityIcon()).append(" ")
                        .append(issue.getTypeIcon()).append(" ")
                        .append(issue.getTitle()).append("\n\n");

                if (issue.getDescription() != null) {
                    sb.append("**描述**: ").append(issue.getDescription()).append("\n\n");
                }

                if (issue.getSuggestion() != null) {
                    sb.append("**建议**: ").append(issue.getSuggestion()).append("\n\n");
                }

                if (issue.getFile() != null) {
                    sb.append("**位置**: `").append(issue.getFile());
                    if (issue.getLine() != null && issue.getLine() > 0) {
                        sb.append(":").append(issue.getLine());
                    }
                    sb.append("`\n\n");
                }

                if (issue.getCodeSnippet() != null) {
                    sb.append("**代码**:\n```\n").append(issue.getCodeSnippet()).append("\n```\n\n");
                }
            }
        } else {
            sb.append("## ✅ 审查结果\n\n");
            sb.append("代码审查完成，未发现明显问题。\n\n");
        }

        sb.append("---\n");
        sb.append("*由 [openai-code-review](https://github.com/Yaeovoi/openai-code-review) 自动生成*\n");

        return sb.toString();
    }

    /**
     * 创建 Git 命令执行器
     */
    private GitCommand createGitCommand() {
        return new GitCommand(
                config.getGithubReviewLogUri(),
                config.getGithubToken(),
                config.getProject(),
                config.getBranch(),
                config.getAuthor(),
                config.getMessage()
        );
    }

    /**
     * 创建 AI 模型
     */
    private IOpenAI createChatModel() {
        String modelCode = config.getChatModelCode();
        String apiHost = config.getApiHost();
        String apiKey = config.getApiKey();
        String protocol = config.getApiProtocol();
        int timeout = config.getApiTimeout();

        logger.info("创建 AI 模型: modelCode={}, apiHost={}, protocol={}, timeout={}s",
            modelCode,
            apiHost != null ? "已配置" : "使用默认",
            protocol,
            timeout / 1000);

        return ChatModelFactory.create(modelCode, apiHost, apiKey, protocol, timeout);
    }

    /**
     * 创建通知器
     */
    private INotification createNotification() {
        String channel = config.getNotification().getChannel();

        switch (channel) {
            case "feishu":
                return new FeiShuNotification(
                        config.getNotification().getFeishuAppId(),
                        config.getNotification().getFeishuAppSecret(),
                        config.getNotification().getFeishuChatId()
                );
            case "dingtalk":
                return new DingTalkNotification(config.getNotification().getDingtalkWebhook());
            case "wecom":
                return new WeComNotification(config.getNotification().getWecomWebhook());
            default:
                throw new IllegalArgumentException("不支持的通知渠道: " + channel);
        }
    }

    /**
     * 构建 commit URL
     */
    private String buildCommitUrl(String repo, String commitSha) {
        if (repo == null || repo.isEmpty() || commitSha == null || commitSha.isEmpty()) {
            return null;
        }
        return "https://github.com/" + repo + "/commit/" + commitSha;
    }

    /**
     * 主入口 - 从环境变量读取配置
     */
    public static void main(String[] args) {
        try {
            CodeReviewConfig config = CodeReviewConfigBuilder.fromEnv();
            CodeReviewRunner runner = new CodeReviewRunner(config);
            runner.run();
            logger.info("代码审查完成!");
        } catch (Exception e) {
            logger.error("代码审查失败", e);
            System.exit(1);
        }
    }
}
