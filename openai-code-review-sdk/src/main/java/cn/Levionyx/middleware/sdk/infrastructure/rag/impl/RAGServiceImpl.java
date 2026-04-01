package cn.Levionyx.middleware.sdk.infrastructure.rag.impl;

import cn.Levionyx.middleware.sdk.domain.model.ChatModel;
import cn.Levionyx.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import cn.Levionyx.middleware.sdk.infrastructure.rag.IRAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码评审服务实现 - 使用 GLM 模型
 */
public class RAGServiceImpl implements IRAGService {

    private static final Logger logger = LoggerFactory.getLogger(RAGServiceImpl.class);

    private final IOpenAI openAI;
    private final String model;

    /**
     * 使用默认模型 (GLM-4-flash)
     */
    public RAGServiceImpl(IOpenAI openAI) {
        this.openAI = openAI;
        this.model = ChatModel.GLM_4_FLASH.getCode();
    }

    /**
     * 自定义模型
     */
    public RAGServiceImpl(IOpenAI openAI, String model) {
        this.openAI = openAI;
        this.model = model;
    }

    @Override
    public String completionsWithRag(String diffCode) throws Exception {
        logger.info("开始 GLM 代码评审");
        logger.info("待评审代码: {}", diffCode);

        // 构建请求
        ChatCompletionRequestDTO requestDTO = new ChatCompletionRequestDTO();
        requestDTO.setModel(model);

        // 构建消息列表
        List<ChatCompletionRequestDTO.Prompt> messages = new ArrayList<>();

        // 系统提示 - 代码评审专家
        ChatCompletionRequestDTO.Prompt systemPrompt = new ChatCompletionRequestDTO.Prompt();
        systemPrompt.setRole("system");
        systemPrompt.setContent("你是一位专业的代码评审专家。请对提交的代码进行审查，关注以下方面：\n" +
                "1. 代码质量和可读性\n" +
                "2. 潜在的bug和错误\n" +
                "3. 性能问题\n" +
                "4. 安全隐患\n" +
                "5. 最佳实践建议\n" +
                "请用中文给出简洁、专业的评审意见。");
        messages.add(systemPrompt);

        // 用户提示 - 提交的代码差异
        ChatCompletionRequestDTO.Prompt userPrompt = new ChatCompletionRequestDTO.Prompt();
        userPrompt.setRole("user");
        userPrompt.setContent("请对以下代码变更进行评审：\n\n" + diffCode);
        messages.add(userPrompt);

        requestDTO.setMessages(messages);

        // 调用 API
        logger.info("调用 GLM API 进行代码评审...");
        ChatCompletionSyncResponseDTO response = openAI.completions(requestDTO);

        // 提取评审结果
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatCompletionSyncResponseDTO.Choice choice = response.getChoices().get(0);
            if (choice != null && choice.getMessage() != null) {
                String content = choice.getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    logger.info("代码评审完成");
                    logger.debug("评审结果: {}", content);
                    return content;
                }
            }
        }

        logger.warn("未获取到评审结果");
        return "代码评审完成，但未获取到具体建议。";
    }
}