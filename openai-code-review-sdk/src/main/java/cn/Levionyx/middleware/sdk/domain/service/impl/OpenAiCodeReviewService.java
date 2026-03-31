package cn.Levionyx.middleware.sdk.domain.service.impl;

import cn.Levionyx.middleware.sdk.domain.service.AbstractOpenAiCodeReviewService;
import cn.Levionyx.middleware.sdk.infrastructure.feishu.FeiShu;
import cn.Levionyx.middleware.sdk.infrastructure.git.GitCommand;
import cn.Levionyx.middleware.sdk.infrastructure.rag.IRAGService;

import java.io.IOException;

public class OpenAiCodeReviewService extends AbstractOpenAiCodeReviewService {

    public OpenAiCodeReviewService(GitCommand gitCommand, FeiShu feiShu, IRAGService ragService) {
        super(gitCommand, feiShu, ragService);
    }

    @Override
    protected String getDiffCode() throws IOException, InterruptedException {
        return gitCommand.diff();
    }

    @Override
    protected String codeReview(String diffCode) throws Exception {
        return ragService.completionsWithRag(diffCode);
    }

    @Override
    protected String recordCodeReview(String recommend) throws Exception {
        return gitCommand.commitAndPush(recommend);
    }

    @Override
    protected void pushMessage(String logUrl, String recommend) throws Exception {
        feiShu.sendMessage(
                logUrl,
                gitCommand.getProject(),
                gitCommand.getBranch(),
                gitCommand.getAuthor(),
                recommend  // 传入代码审查结果
        );
    }
}