package cn.Levionyx.middleware.sdk.domain.model.review;

/**
 * 审查问题
 */
public class ReviewIssue {

    /** 问题类型: security, bug, performance, style, maintainability */
    private String type;

    /** 严重程度: critical, high, medium, low */
    private String severity;

    /** 问题标题 */
    private String title;

    /** 问题描述 */
    private String description;

    /** 修复建议 */
    private String suggestion;

    /** 文件路径 */
    private String file;

    /** 行号 */
    private Integer line;

    /** 代码片段 */
    private String codeSnippet;

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }

    /**
     * 获取类型图标
     */
    public String getTypeIcon() {
        if (type == null) return "📝";
        switch (type.toLowerCase()) {
            case "security":
                return "🔒";
            case "bug":
                return "🐛";
            case "performance":
                return "⚡";
            case "style":
                return "🎨";
            case "maintainability":
                return "🔧";
            default:
                return "📝";
        }
    }

    /**
     * 获取严重程度图标
     */
    public String getSeverityIcon() {
        if (severity == null) return "🟢";
        switch (severity.toLowerCase()) {
            case "critical":
                return "🔴";
            case "high":
                return "🟠";
            case "medium":
                return "🟡";
            case "low":
                return "🟢";
            default:
                return "🟢";
        }
    }

    /**
     * 获取严重程度中文描述
     */
    public String getSeverityLabel() {
        if (severity == null) return "低";
        switch (severity.toLowerCase()) {
            case "critical":
                return "严重";
            case "high":
                return "高危";
            case "medium":
                return "中危";
            case "low":
                return "低";
            default:
                return "低";
        }
    }
}