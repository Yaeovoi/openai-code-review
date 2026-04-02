package cn.Levionyx.middleware.sdk.domain.service;

import cn.Levionyx.middleware.sdk.domain.model.review.CodeReviewResult;
import cn.Levionyx.middleware.sdk.infrastructure.feishu.FeiShu;
import cn.Levionyx.middleware.sdk.infrastructure.git.GitCommand;
import cn.Levionyx.middleware.sdk.infrastructure.github.GitHubPrComment;
import cn.Levionyx.middleware.sdk.infrastructure.notification.INotification;
import cn.Levionyx.middleware.sdk.infrastructure.rag.IRAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 代码审查服务抽象类
 */
public abstract class AbstractOpenAiCodeReviewService implements IOpenAiCodeReviewService {

    private final Logger logger = LoggerFactory.getLogger(AbstractOpenAiCodeReviewService.class);

    protected final GitCommand gitCommand;
    protected final FeiShu feiShu;
    protected final INotification notification;
    protected final IRAGService ragService;

    public AbstractOpenAiCodeReviewService(GitCommand gitCommand, FeiShu feiShu, INotification notification, IRAGService ragService) {
        this.gitCommand = gitCommand;
        this.feiShu = feiShu;
        this.notification = notification;
        this.ragService = ragService;
    }

    @Override
    public void exec() {
        try {
            // 1. 获取提交代码
            String diffCode = getDiffCode();

            // 2. 开始评审代码（返回结构化结果）
            CodeReviewResult result = codeReview(diffCode);

            // 3. 发布 PR 评论（如果配置了）
            if (shouldPostPrComment()) {
                postPrComment(result, diffCode);
            }

            // 4. 记录评审结果；返回日志地址
            String logUrl = recordCodeReview(result);

            // 5. 发送消息通知
            pushMessage(logUrl, result);

        } catch (Exception e) {
            logger.error("openai-code-review error", e);
        }
    }

    protected abstract String getDiffCode() throws IOException, InterruptedException;

    protected abstract CodeReviewResult codeReview(String diffCode) throws Exception;

    protected abstract String recordCodeReview(CodeReviewResult result) throws Exception;

    protected abstract void pushMessage(String logUrl, CodeReviewResult result) throws Exception;

    /**
     * 判断是否应该发布 PR 评论
     */
    protected boolean shouldPostPrComment() {
        String prNumber = System.getenv("GITHUB_PR_NUMBER");
        return prNumber != null && !prNumber.isEmpty();
    }

    /**
     * 发布 PR 评论
     */
    protected void postPrComment(CodeReviewResult result, String diffCode) {
        try {
            String githubToken = System.getenv("GITHUB_TOKEN");
            String repo = System.getenv("GITHUB_REPOSITORY");
            String prNumberStr = System.getenv("GITHUB_PR_NUMBER");
            String sha = System.getenv("GITHUB_SHA");

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
}