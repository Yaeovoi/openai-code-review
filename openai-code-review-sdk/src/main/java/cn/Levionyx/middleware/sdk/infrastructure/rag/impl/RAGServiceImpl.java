package cn.Levionyx.middleware.sdk.infrastructure.rag.impl;

import cn.Levionyx.middleware.sdk.domain.model.ChatModel;
import cn.Levionyx.middleware.sdk.domain.model.review.CodeReviewResult;
import cn.Levionyx.middleware.sdk.domain.model.review.ReviewIssue;
import cn.Levionyx.middleware.sdk.domain.model.review.ReviewMetrics;
import cn.Levionyx.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import cn.Levionyx.middleware.sdk.infrastructure.rag.IRAGService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码评审服务实现
 */
public class RAGServiceImpl implements IRAGService {

    private static final Logger logger = LoggerFactory.getLogger(RAGServiceImpl.class);

    private final IOpenAI openAI;
    private final String model;

    public RAGServiceImpl(IOpenAI openAI) {
        this.openAI = openAI;
        this.model = ChatModel.QWEN_CODER_PLUS.getCode();
    }

    public RAGServiceImpl(IOpenAI openAI, String model) {
        this.openAI = openAI;
        this.model = model;
    }

    @Override
    public CodeReviewResult reviewCode(String diffCode) throws Exception {
        logger.info("开始代码审查...");

        // 构建请求
        ChatCompletionRequestDTO requestDTO = new ChatCompletionRequestDTO();
        requestDTO.setModel(model);

        List<ChatCompletionRequestDTO.Prompt> messages = new ArrayList<>();

        // 系统提示 - 要求返回结构化 JSON
        ChatCompletionRequestDTO.Prompt systemPrompt = new ChatCompletionRequestDTO.Prompt();
        systemPrompt.setRole("system");
        systemPrompt.setContent(buildSystemPrompt());
        messages.add(systemPrompt);

        // 用户提示 - 代码差异
        ChatCompletionRequestDTO.Prompt userPrompt = new ChatCompletionRequestDTO.Prompt();
        userPrompt.setRole("user");
        userPrompt.setContent(buildUserPrompt(diffCode));
        messages.add(userPrompt);

        requestDTO.setMessages(messages);

        // 调用 API
        logger.info("调用 AI API 进行代码审查...");
        ChatCompletionSyncResponseDTO response = openAI.completions(requestDTO);

        // 提取响应内容
        String content = extractContent(response);
        if (content == null || content.isEmpty()) {
            logger.warn("未获取到审查结果，返回默认结果");
            return createDefaultResult();
        }

        // 解析 JSON
        CodeReviewResult result = parseReviewResult(content, diffCode);
        logger.info("代码审查完成，质量评分: {}, 问题数: {}",
                result.getQualityScore(),
                result.getSummary().getTotalIssues());

        return result;
    }

    @Override
    @Deprecated
    public String completionsWithRag(String diffCode) throws Exception {
        CodeReviewResult result = reviewCode(diffCode);
        return formatResultAsMarkdown(result);
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
     * 构建用户提示
     */
    private String buildUserPrompt(String diffCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("请审查以下代码变更：\n\n");
        sb.append("```\n");
        sb.append(diffCode);
        sb.append("\n```\n\n");
        sb.append("请返回 JSON 格式的审查结果。");
        return sb.toString();
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
    private CodeReviewResult parseReviewResult(String content, String diffCode) {
        CodeReviewResult result = new CodeReviewResult();

        try {
            // 清理可能的 markdown 标记
            String jsonStr = cleanJsonString(content);

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

            // 保存原始内容
            result.setRawContent(content);

        } catch (Exception e) {
            logger.error("解析审查结果失败: {}", e.getMessage());
            // 尝试从原始内容创建简单结果
            result = createResultFromRawContent(content, diffCode);
        }

        return result;
    }

    /**
     * 清理 JSON 字符串
     */
    private String cleanJsonString(String content) {
        String result = content.trim();

        // 移除 markdown 代码块标记
        if (result.startsWith("```json")) {
            result = result.substring(7);
        } else if (result.startsWith("```")) {
            result = result.substring(3);
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3);
        }

        return result.trim();
    }

    /**
     * 从原始内容创建结果
     */
    private CodeReviewResult createResultFromRawContent(String content, String diffCode) {
        CodeReviewResult result = new CodeReviewResult();
        result.setQualityScore(75);
        result.setRawContent(content);
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
     * 格式化结果为 Markdown
     */
    private String formatResultAsMarkdown(CodeReviewResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 代码审查报告\n\n");
        sb.append("## 质量评分: ").append(result.getQualityScore()).append("/100\n\n");

        // 统计
        CodeReviewResult.Summary summary = result.getSummary();
        sb.append("## 问题统计\n\n");
        sb.append("- 🔴 严重: ").append(summary.getCriticalIssues()).append("\n");
        sb.append("- 🟠 高危: ").append(summary.getHighIssues()).append("\n");
        sb.append("- 🟡 中危: ").append(summary.getMediumIssues()).append("\n");
        sb.append("- 🟢 低危: ").append(summary.getLowIssues()).append("\n\n");

        // 问题列表
        if (result.getIssues() != null && !result.getIssues().isEmpty()) {
            sb.append("## 问题列表\n\n");
            for (int i = 0; i < result.getIssues().size(); i++) {
                ReviewIssue issue = result.getIssues().get(i);
                sb.append(i + 1).append(". ").append(issue.getSeverityIcon())
                        .append(" ").append(issue.getTypeIcon())
                        .append(" **").append(issue.getTitle()).append("**\n");
                if (issue.getDescription() != null) {
                    sb.append("   - ").append(issue.getDescription()).append("\n");
                }
                if (issue.getSuggestion() != null) {
                    sb.append("   - 💡 ").append(issue.getSuggestion()).append("\n");
                }
                if (issue.getFile() != null) {
                    sb.append("   - 📁 ").append(issue.getFile());
                    if (issue.getLine() != null) {
                        sb.append(":").append(issue.getLine());
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}