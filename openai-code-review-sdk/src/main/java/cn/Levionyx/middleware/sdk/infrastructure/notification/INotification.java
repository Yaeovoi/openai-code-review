package cn.Levionyx.middleware.sdk.infrastructure.notification;

/**
 * 通知接口 - 支持多种通知渠道
 */
public interface INotification {

    /**
     * 发送代码审查通知
     *
     * @param logUrl  审查详情链接
     * @param project 项目名称
     * @param branch  分支名称
     * @param author  提交作者
     * @param reviewContent 审查内容
     * @throws Exception 发送失败
     */
    void send(String logUrl, String project, String branch, String author, String reviewContent) throws Exception;

    /**
     * 获取通知渠道名称
     */
    String getChannelName();
}