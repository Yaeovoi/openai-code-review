package cn.Levionyx.middleware.sdk.infrastructure.github;

import cn.Levionyx.middleware.sdk.domain.model.review.CodeReviewResult;
import cn.Levionyx.middleware.sdk.domain.model.review.ReviewIssue;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub PR 评论服务
 * 用于在 PR 中发布代码审查结果
 */
public class GitHubPrComment {

    private static final Logger logger = LoggerFactory.getLogger(GitHubPrComment.class);

    private final String githubToken;
    private final String repoOwner;
    private final String repoName;
    private final int prNumber;
    private final HttpClient httpClient;
    private final String commitSha;

    public GitHubPrComment(String githubToken, String repoFullName, int prNumber, String commitSha) {
        this.githubToken = githubToken;
        this.commitSha = commitSha;

        String[] parts = repoFullName.split("/");
        this.repoOwner = parts[0];
        this.repoName = parts.length > 1 ? parts[1] : parts[0];
        this.prNumber = prNumber;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 发布 PR 评论
     */
    public void postReviewComment(CodeReviewResult result, String diffContent) {
        try {
            // 1. 发布总体评论
            postSummaryComment(result);

            // 2. 发布行内评论（针对具体问题）
            if (result.getIssues() != null && !result.getIssues().isEmpty()) {
                postLineComments(result, diffContent);
            }

            logger.info("PR 评论发布成功");
        } catch (Exception e) {
            logger.error("发布 PR 评论失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发布总体评论
     */
    private void postSummaryComment(CodeReviewResult result) throws Exception {
        String body = buildSummaryBody(result);

        String url = String.format("https://api.github.com/repos/%s/%s/issues/%d/comments",
                repoOwner, repoName, prNumber);

        JSONObject requestBody = new JSONObject();
        requestBody.put("body", body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "token " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            logger.error("发布总体评论失败: {}", response.body());
        } else {
            logger.info("总体评论发布成功");
        }
    }

    /**
     * 发布行内评论
     */
    private void postLineComments(CodeReviewResult result, String diffContent) throws Exception {
        // 解析 diff 获取文件信息
        Map<String, GitDiffParser.DiffFile> files = GitDiffParser.parse(diffContent);

        // 按文件分组问题
        Map<String, List<ReviewIssue>> issuesByFile = new HashMap<>();
        for (ReviewIssue issue : result.getIssues()) {
            if (issue.getFile() != null) {
                issuesByFile.computeIfAbsent(issue.getFile(), k -> new ArrayList<>()).add(issue);
            }
        }

        // 为每个文件的问题发布评论
        for (Map.Entry<String, List<ReviewIssue>> entry : issuesByFile.entrySet()) {
            String filePath = entry.getKey();
            List<ReviewIssue> issues = entry.getValue();

            GitDiffParser.DiffFile diffFile = files.get(filePath);
            if (diffFile == null) {
                logger.warn("文件 {} 未在 diff 中找到，跳过行内评论", filePath);
                continue;
            }

            // 找到合适的行号
            Integer targetLine = findSuitableLine(diffFile, issues);
            if (targetLine == null) {
                targetLine = diffFile.getChangedLines().isEmpty() ? 1 : diffFile.getChangedLines().get(0);
            }

            // 发布该文件的评论
            postFileComment(filePath, targetLine, issues);
        }
    }

    /**
     * 找到合适的评论行号
     */
    private Integer findSuitableLine(GitDiffParser.DiffFile diffFile, List<ReviewIssue> issues) {
        for (ReviewIssue issue : issues) {
            if (issue.getLine() != null && issue.getLine() > 0) {
                // 如果问题有行号，检查是否在变更行中
                if (diffFile.getChangedLines().contains(issue.getLine())) {
                    return issue.getLine();
                }
            }
        }
        // 返回第一个变更行
        return diffFile.getChangedLines().isEmpty() ? null : diffFile.getChangedLines().get(0);
    }

    /**
     * 发布文件评论
     */
    private void postFileComment(String filePath, int line, List<ReviewIssue> issues) throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("### 📋 ").append(filePath).append("\n\n");

        for (ReviewIssue issue : issues) {
            body.append(issue.getSeverityIcon()).append(" **").append(issue.getSeverityLabel()).append("** - ")
                    .append(issue.getTypeIcon()).append(" ").append(issue.getTitle()).append("\n");
            if (issue.getDescription() != null) {
                body.append("> ").append(issue.getDescription()).append("\n");
            }
            if (issue.getSuggestion() != null) {
                body.append("💡 ").append(issue.getSuggestion()).append("\n");
            }
            body.append("\n");
        }

        String url = String.format("https://api.github.com/repos/%s/%s/pulls/%d/comments",
                repoOwner, repoName, prNumber);

        JSONObject requestBody = new JSONObject();
        requestBody.put("body", body.toString());
        requestBody.put("path", filePath);
        requestBody.put("line", line);
        requestBody.put("side", "RIGHT");
        requestBody.put("commit_id", commitSha);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "token " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            logger.warn("发布文件评论失败 ({}): {}", filePath, response.body());
        } else {
            logger.info("文件评论发布成功: {}", filePath);
        }
    }

    /**
     * 构建总体评论内容
     */
    private String buildSummaryBody(CodeReviewResult result) {
        StringBuilder sb = new StringBuilder();

        // 标题和质量评分
        sb.append("## 🤖 AI 代码审查结果\n\n");
        sb.append("### 📊 质量评分: ").append(result.getQualityScore()).append("/100\n\n");

        // 评分徽章
        String badge = getQualityBadge(result.getQualityScore());
        sb.append(badge).append("\n\n");

        // 统计信息
        CodeReviewResult.Summary summary = result.getSummary();
        sb.append("### 📈 统计信息\n\n");
        sb.append("| 严重程度 | 数量 |\n");
        sb.append("|----------|------|\n");
        sb.append("| 🔴 严重 | ").append(summary.getCriticalIssues()).append(" |\n");
        sb.append("| 🟠 高危 | ").append(summary.getHighIssues()).append(" |\n");
        sb.append("| 🟡 中危 | ").append(summary.getMediumIssues()).append(" |\n");
        sb.append("| 🟢 低危 | ").append(summary.getLowIssues()).append(" |\n");
        sb.append("| **总计** | **").append(summary.getTotalIssues()).append("** |\n\n");

        // 维度评分
        if (result.getMetrics() != null) {
            sb.append("### 📐 维度评分\n\n");
            sb.append("| 维度 | 评分 |\n");
            sb.append("|------|------|\n");
            sb.append("| 🔒 安全性 | ").append(result.getMetrics().getSecurity()).append("/100 |\n");
            sb.append("| ⚡ 性能 | ").append(result.getMetrics().getPerformance()).append("/100 |\n");
            sb.append("| 🔧 可维护性 | ").append(result.getMetrics().getMaintainability()).append("/100 |\n");
            sb.append("| 🎨 代码风格 | ").append(result.getMetrics().getStyle()).append("/100 |\n\n");
        }

        // 问题列表
        if (result.getIssues() != null && !result.getIssues().isEmpty()) {
            sb.append("### 📋 问题列表\n\n");

            // 按严重程度分组
            List<ReviewIssue> criticalAndHigh = new ArrayList<>();
            List<ReviewIssue> medium = new ArrayList<>();
            List<ReviewIssue> low = new ArrayList<>();

            for (ReviewIssue issue : result.getIssues()) {
                String sev = issue.getSeverity();
                if ("critical".equalsIgnoreCase(sev) || "high".equalsIgnoreCase(sev)) {
                    criticalAndHigh.add(issue);
                } else if ("medium".equalsIgnoreCase(sev)) {
                    medium.add(issue);
                } else {
                    low.add(issue);
                }
            }

            if (!criticalAndHigh.isEmpty()) {
                sb.append("#### 🔴 需要关注 (").append(criticalAndHigh.size()).append(")\n\n");
                for (ReviewIssue issue : criticalAndHigh) {
                    sb.append(formatIssueItem(issue)).append("\n");
                }
                sb.append("\n");
            }

            if (!medium.isEmpty()) {
                sb.append("#### 🟡 建议修复 (").append(medium.size()).append(")\n\n");
                for (ReviewIssue issue : medium) {
                    sb.append(formatIssueItem(issue)).append("\n");
                }
                sb.append("\n");
            }

            if (!low.isEmpty()) {
                sb.append("#### 🟢 可选优化 (").append(low.size()).append(")\n\n");
                for (ReviewIssue issue : low) {
                    sb.append(formatIssueItem(issue)).append("\n");
                }
                sb.append("\n");
            }
        } else {
            sb.append("✅ 未发现问题，代码质量良好！\n\n");
        }

        // 页脚
        sb.append("---\n");
        sb.append("*由 [openai-code-review](https://github.com/Yaeovoi/openai-code-review) 自动生成*\n");

        return sb.toString();
    }

    /**
     * 格式化问题项
     */
    private String formatIssueItem(ReviewIssue issue) {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(issue.getSeverityIcon()).append(" ").append(issue.getTypeIcon());
        sb.append(" **").append(issue.getTitle()).append("**");

        if (issue.getFile() != null) {
            sb.append(" [`").append(issue.getFile());
            if (issue.getLine() != null && issue.getLine() > 0) {
                sb.append(":").append(issue.getLine());
            }
            sb.append("`]");
        }

        sb.append("\n");

        if (issue.getSuggestion() != null) {
            sb.append("  > 💡 ").append(issue.getSuggestion()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 获取质量评分徽章
     */
    private String getQualityBadge(int score) {
        if (score >= 90) {
            return "![Excellent](https://img.shields.io/badge/质量-优秀-brightgreen)";
        } else if (score >= 70) {
            return "![Good](https://img.shields.io/badge/质量-良好-green)";
        } else if (score >= 50) {
            return "![Fair](https://img.shields.io/badge/质量-一般-yellow)";
        } else {
            return "![Poor](https://img.shields.io/badge/质量-较差-red)";
        }
    }
}