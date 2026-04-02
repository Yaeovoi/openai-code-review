package cn.Levionyx.middleware.sdk.domain.model.review;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码审查结果
 */
public class CodeReviewResult {

    /** 质量评分 0-100 */
    private int qualityScore;

    /** 问题汇总 */
    private Summary summary;

    /** 问题列表 */
    private List<ReviewIssue> issues;

    /** 维度评分 */
    private ReviewMetrics metrics;

    /** 原始审查内容（用于通知） */
    private String rawContent;

    public CodeReviewResult() {
        this.issues = new ArrayList<>();
        this.summary = new Summary();
        this.metrics = new ReviewMetrics();
    }

    // Getters and Setters
    public int getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(int qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public List<ReviewIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ReviewIssue> issues) {
        this.issues = issues;
        updateSummary();
    }

    public ReviewMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(ReviewMetrics metrics) {
        this.metrics = metrics;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    /**
     * 添加问题
     */
    public void addIssue(ReviewIssue issue) {
        if (this.issues == null) {
            this.issues = new ArrayList<>();
        }
        this.issues.add(issue);
        updateSummary();
    }

    /**
     * 更新汇总统计
     */
    private void updateSummary() {
        if (summary == null) {
            summary = new Summary();
        }
        summary.setTotalIssues(issues != null ? issues.size() : 0);
        summary.setCriticalIssues(countBySeverity("critical"));
        summary.setHighIssues(countBySeverity("high"));
        summary.setMediumIssues(countBySeverity("medium"));
        summary.setLowIssues(countBySeverity("low"));
    }

    private int countBySeverity(String severity) {
        if (issues == null) return 0;
        return (int) issues.stream()
                .filter(i -> severity.equalsIgnoreCase(i.getSeverity()))
                .count();
    }

    /**
     * 问题汇总
     */
    public static class Summary {
        private int totalIssues;
        private int criticalIssues;
        private int highIssues;
        private int mediumIssues;
        private int lowIssues;

        public int getTotalIssues() {
            return totalIssues;
        }

        public void setTotalIssues(int totalIssues) {
            this.totalIssues = totalIssues;
        }

        public int getCriticalIssues() {
            return criticalIssues;
        }

        public void setCriticalIssues(int criticalIssues) {
            this.criticalIssues = criticalIssues;
        }

        public int getHighIssues() {
            return highIssues;
        }

        public void setHighIssues(int highIssues) {
            this.highIssues = highIssues;
        }

        public int getMediumIssues() {
            return mediumIssues;
        }

        public void setMediumIssues(int mediumIssues) {
            this.mediumIssues = mediumIssues;
        }

        public int getLowIssues() {
            return lowIssues;
        }

        public void setLowIssues(int lowIssues) {
            this.lowIssues = lowIssues;
        }
    }
}