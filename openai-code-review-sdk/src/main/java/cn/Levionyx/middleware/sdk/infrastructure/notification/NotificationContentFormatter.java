package cn.Levionyx.middleware.sdk.infrastructure.notification;

import cn.Levionyx.middleware.sdk.domain.model.review.CodeReviewResult;
import cn.Levionyx.middleware.sdk.domain.model.review.ReviewIssue;

import java.util.List;

/**
 * 通知内容格式化工具
 */
public final class NotificationContentFormatter {

    private NotificationContentFormatter() {
    }

    /**
     * 格式化通知摘要，包含问题统计和审查意见。
     */
    public static String formatReviewSummary(CodeReviewResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("**质量评分**: ").append(result.getQualityScore()).append("/100\n\n");

        CodeReviewResult.Summary summary = result.getSummary();
        if (summary == null || summary.getTotalIssues() <= 0) {
            sb.append("✅ 未发现明显问题\n");
            return sb.toString();
        }

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

        List<ReviewIssue> issues = result.getIssues();
        if (issues == null || issues.isEmpty()) {
            return sb.toString();
        }

        sb.append("\n**代码审查意见**\n");
        for (int i = 0; i < issues.size(); i++) {
            appendIssue(sb, i + 1, issues.get(i));
        }

        return sb.toString();
    }

    private static void appendIssue(StringBuilder sb, int index, ReviewIssue issue) {
        sb.append(index).append(". ")
                .append(issue.getSeverityIcon()).append(" ")
                .append(nullToEmpty(issue.getSeverityLabel()));

        String typeLabel = getTypeLabel(issue.getType());
        if (!typeLabel.isEmpty()) {
            sb.append(" / ").append(issue.getTypeIcon()).append(" ").append(typeLabel);
        }

        String location = getLocation(issue);
        if (!location.isEmpty()) {
            sb.append(" / `").append(location).append("`");
        }
        sb.append("\n");

        if (!isBlank(issue.getTitle())) {
            sb.append("标题: ").append(issue.getTitle().trim()).append("\n");
        }
        if (!isBlank(issue.getDescription())) {
            sb.append("描述: ").append(issue.getDescription().trim()).append("\n");
        }
        if (!isBlank(issue.getSuggestion())) {
            sb.append("建议: ").append(issue.getSuggestion().trim()).append("\n");
        }
        sb.append("\n");
    }

    private static String getLocation(ReviewIssue issue) {
        if (issue == null || isBlank(issue.getFile())) {
            return "";
        }

        StringBuilder location = new StringBuilder(issue.getFile().trim());
        if (issue.getLine() != null && issue.getLine() > 0) {
            location.append(":").append(issue.getLine());
        }
        return location.toString();
    }

    private static String getTypeLabel(String type) {
        if (type == null) {
            return "";
        }
        switch (type.toLowerCase()) {
            case "security":
                return "安全";
            case "bug":
                return "Bug";
            case "performance":
                return "性能";
            case "style":
                return "风格";
            case "maintainability":
                return "可维护性";
            default:
                return type;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
