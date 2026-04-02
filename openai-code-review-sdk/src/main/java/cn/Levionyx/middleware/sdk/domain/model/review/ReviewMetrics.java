package cn.Levionyx.middleware.sdk.domain.model.review;

/**
 * 审查维度评分
 */
public class ReviewMetrics {

    /** 安全评分 0-100 */
    private int security;

    /** 性能评分 0-100 */
    private int performance;

    /** 可维护性评分 0-100 */
    private int maintainability;

    /** 代码风格评分 0-100 */
    private int style;

    public ReviewMetrics() {
        this.security = 80;
        this.performance = 80;
        this.maintainability = 80;
        this.style = 80;
    }

    // Getters and Setters
    public int getSecurity() {
        return security;
    }

    public void setSecurity(int security) {
        this.security = security;
    }

    public int getPerformance() {
        return performance;
    }

    public void setPerformance(int performance) {
        this.performance = performance;
    }

    public int getMaintainability() {
        return maintainability;
    }

    public void setMaintainability(int maintainability) {
        this.maintainability = maintainability;
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }
}