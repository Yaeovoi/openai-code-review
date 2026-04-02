package cn.Levionyx.middleware.sdk.domain.service.impl;

import cn.Levionyx.middleware.sdk.domain.model.review.CodeReviewResult;
import cn.Levionyx.middleware.sdk.domain.model.review.ReviewIssue;
import cn.Levionyx.middleware.sdk.domain.service.AbstractOpenAiCodeReviewService;
import cn.Levionyx.middleware.sdk.infrastructure.feishu.FeiShu;
import cn.Levionyx.middleware.sdk.infrastructure.git.GitCommand;
import cn.Levionyx.middleware.sdk.infrastructure.notification.INotification;
import cn.Levionyx.middleware.sdk.infrastructure.rag.IRAGService;

import java.io.IOException;

public class OpenAiCodeReviewService extends AbstractOpenAiCodeReviewService {

    public OpenAiCodeReviewService(GitCommand gitCommand, FeiShu feiShu, INotification notification, IRAGService ragService) {
        super(gitCommand, feiShu, notification, ragService);
    }

    @Override
    protected String getDiffCode() throws IOException, InterruptedException {
        return gitCommand.diff();
    }

    @Override
    protected CodeReviewResult codeReview(String diffCode) throws Exception {
        return ragService.reviewCode(diffCode);
    }

    @Override
    protected String recordCodeReview(CodeReviewResult result) throws Exception {
        String content = formatResultAsMarkdown(result);
        return gitCommand.commitAndPush(content);
    }

    @Override
    protected void pushMessage(String logUrl, CodeReviewResult result) throws Exception {
        String summary = formatShortSummary(result);

        // 使用新的通知接口
        if (notification != null) {
            String commitUrl = buildCommitUrl(gitCommand.getProject(), System.getenv("GITHUB_SHA"));
            notification.send(logUrl, gitCommand.getProject(), gitCommand.getBranch(),
                    gitCommand.getAuthor(), gitCommand.getMessage(), commitUrl, summary);
        }

        // 兼容旧的飞书通知
        if (feiShu != null) {
            feiShu.sendMessage(
                    logUrl,
                    gitCommand.getProject(),
                    gitCommand.getBranch(),
                    gitCommand.getAuthor(),
                    summary
            );
        }
    }

    private String buildCommitUrl(String repo, String commitSha) {
        if (repo == null || commitSha == null) {
            return null;
        }
        return "https://github.com/" + repo + "/commit/" + commitSha;
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

        // 页脚
        sb.append("---\n");
        sb.append("*由 [openai-code-review](https://github.com/Yaeovoi/openai-code-review) 自动生成*\n");

        return sb.toString();
    }

    /**
     * 格式化通知消息
     */
    private String formatNotificationMessage(String logUrl, CodeReviewResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 🤖 代码审查结果\n\n");
        sb.append("**质量评分**: ").append(result.getQualityScore()).append("/100\n\n");

        CodeReviewResult.Summary summary = result.getSummary();
        sb.append("**问题统计**: ");
        sb.append("🔴 ").append(summary.getCriticalIssues()).append(" | ");
        sb.append("🟠 ").append(summary.getHighIssues()).append(" | ");
        sb.append("🟡 ").append(summary.getMediumIssues()).append(" | ");
        sb.append("🟢 ").append(summary.getLowIssues()).append("\n\n");

        sb.append("**详情**: [查看完整报告](").append(logUrl).append(")\n");

        return sb.toString();
    }

    /**
     * 格式化简短摘要（用于飞书卡片）
     */
    private String formatShortSummary(CodeReviewResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("**质量评分**: ").append(result.getQualityScore()).append("/100\n\n");

        CodeReviewResult.Summary summary = result.getSummary();
        if (summary.getTotalIssues() > 0) {
            sb.append("**发现问题**: ").append(summary.getTotalIssues()).append(" 个\n");
            if (summary.getCriticalIssues() > 0) {
                sb.append("- 🔴 严重: ").append(summary.getCriticalIssues()).append("\n");
            }
            if (summary.getHighIssues() > 0) {
                sb.append("- 🟠 高危: ").append(summary.getHighIssues()).append("\n");
            }
            if (summary.getMediumIssues() > 0) {
                sb.append("- 🟡 中危: ").append(summary.getMediumIssues()).append("\n");
            }
            if (summary.getLowIssues() > 0) {
                sb.append("- 🟢 低危: ").append(summary.getLowIssues()).append("\n");
            }
        } else {
            sb.append("✅ 未发现明显问题\n");
        }

        return sb.toString();
    }
}