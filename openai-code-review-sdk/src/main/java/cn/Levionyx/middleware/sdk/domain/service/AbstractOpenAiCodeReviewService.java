package cn.Levionyx.middleware.sdk.domain.service;

import cn.Levionyx.middleware.sdk.infrastructure.feishu.FeiShu;
import cn.Levionyx.middleware.sdk.infrastructure.git.GitCommand;
import cn.Levionyx.middleware.sdk.infrastructure.rag.IRAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public abstract class AbstractOpenAiCodeReviewService implements IOpenAiCodeReviewService {

    private final Logger logger = LoggerFactory.getLogger(AbstractOpenAiCodeReviewService.class);

    protected final GitCommand gitCommand;
    protected final FeiShu feiShu;
    protected final IRAGService ragService;

    public AbstractOpenAiCodeReviewService(GitCommand gitCommand, FeiShu feiShu, IRAGService ragService) {
        this.gitCommand = gitCommand;
        this.feiShu = feiShu;
        this.ragService = ragService;
    }

    @Override
    public void exec() {
        try {
            // 1. 获取提交代码
            String diffCode = getDiffCode();
            // 2. 开始评审代码
            String recommend = codeReview(diffCode);
            // 3. 记录评审结果；返回日志地址
            String logUrl = recordCodeReview(recommend);
            // 4. 发送消息通知；传入审查结果
            pushMessage(logUrl, recommend);
        } catch (Exception e) {
            logger.error("openai-code-review error", e);
        }

    }

    protected abstract String getDiffCode() throws IOException, InterruptedException;

    protected abstract String codeReview(String diffCode) throws Exception;

    protected abstract String recordCodeReview(String recommend) throws Exception;

    protected abstract void pushMessage(String logUrl, String recommend) throws Exception;

}