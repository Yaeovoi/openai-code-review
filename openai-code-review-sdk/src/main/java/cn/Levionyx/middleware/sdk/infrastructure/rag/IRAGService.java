package cn.Levionyx.middleware.sdk.infrastructure.rag;

import cn.Levionyx.middleware.sdk.domain.model.review.CodeReviewResult;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;

public interface IRAGService {
    /**
     * 执行代码审查，返回结构化结果
     * @param diffCode 代码差异
     * @return 结构化审查结果
     * @throws Exception 异常
     */
    CodeReviewResult reviewCode(String diffCode) throws Exception;

    /**
     * @deprecated 使用 {@link #reviewCode(String)} 替代
     */
    @Deprecated
    String completionsWithRag(String message) throws Exception;
}
