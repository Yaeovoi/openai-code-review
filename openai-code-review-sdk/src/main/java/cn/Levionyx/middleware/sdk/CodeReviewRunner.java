package cn.Levionyx.middleware.sdk;

import cn.Levionyx.middleware.sdk.config.CodeReviewConfig;
import cn.Levionyx.middleware.sdk.config.CodeReviewConfigBuilder;
import cn.Levionyx.middleware.sdk.infrastructure.git.GitCommand;
import cn.Levionyx.middleware.sdk.infrastructure.notification.INotification;
import cn.Levionyx.middleware.sdk.infrastructure.notification.impl.DingTalkNotification;
import cn.Levionyx.middleware.sdk.infrastructure.notification.impl.FeiShuNotification;
import cn.Levionyx.middleware.sdk.infrastructure.notification.impl.WeComNotification;
import cn.Levionyx.middleware.sdk.infrastructure.openai.ChatModelFactory;
import cn.Levionyx.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码审查运行器 - 支持配置化的主入口
 */
public class CodeReviewRunner {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewRunner.class);

    private final CodeReviewConfig config;
    private final GitCommand gitCommand;
    private final IOpenAI chatModel;
    private final INotification notification;

    public CodeReviewRunner(CodeReviewConfig config) {
        this.config = config;
        this.gitCommand = createGitCommand();
        this.chatModel = createChatModel();
        this.notification = createNotification();
    }

    /**
     * 执行代码审查
     */
    public void run() {
        try {
            logger.info("开始代码审查...");

            // 1. 获取代码差异
            String diffCode = gitCommand.diff();
            logger.info("获取到代码差异, 长度: {}", diffCode.length());

            // 2. AI 审查
            String reviewResult = review(diffCode);
            logger.info("代码审查完成");

            // 3. 保存审查结果
            String logUrl = gitCommand.commitAndPush(reviewResult);
            logger.info("审查结果已保存: {}", logUrl);

            // 4. 发送通知
            notification.send(logUrl, config.getProject(), config.getBranch(), config.getAuthor(), config.getMessage(), reviewResult);
            logger.info("代码审查通知已发送");

        } catch (Exception e) {
            logger.error("代码审查失败", e);
            throw new RuntimeException("代码审查失败", e);
        }
    }

    /**
     * 使用 AI 模型进行代码审查
     */
    private String review(String diffCode) throws Exception {
        ChatCompletionRequestDTO request = new ChatCompletionRequestDTO();
        request.setModel(config.getChatModelCode());  // 直接使用用户配置的模型代码

        List<ChatCompletionRequestDTO.Prompt> messages = new ArrayList<>();

        // 系统提示
        ChatCompletionRequestDTO.Prompt systemPrompt = new ChatCompletionRequestDTO.Prompt();
        systemPrompt.setRole("system");
        systemPrompt.setContent(config.getReviewRules().generateSystemPrompt());
        messages.add(systemPrompt);

        // 用户提示
        ChatCompletionRequestDTO.Prompt userPrompt = new ChatCompletionRequestDTO.Prompt();
        userPrompt.setRole("user");
        userPrompt.setContent("请对以下代码变更进行评审：\n\n" + diffCode);
        messages.add(userPrompt);

        request.setMessages(messages);

        ChatCompletionSyncResponseDTO response = chatModel.completions(request);

        // 安全地提取响应内容
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatCompletionSyncResponseDTO.Choice choice = response.getChoices().get(0);
            if (choice != null && choice.getMessage() != null) {
                String content = choice.getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    return content;
                }
            }
        }

        logger.warn("AI 模型返回空响应，返回默认消息");
        return "代码审查完成，但未获取到具体建议。";
    }

    /**
     * 创建 Git 命令执行器
     */
    private GitCommand createGitCommand() {
        return new GitCommand(
                config.getGithubReviewLogUri(),
                config.getGithubToken(),
                config.getProject(),
                config.getBranch(),
                config.getAuthor(),
                config.getMessage()
        );
    }

    /**
     * 创建 AI 模型
     */
    private IOpenAI createChatModel() {
        String modelCode = config.getChatModelCode();
        String apiHost = config.getApiHost();
        String apiKey = config.getApiKey();
        String protocol = config.getApiProtocol();
        int timeout = config.getApiTimeout();

        logger.info("创建 AI 模型: modelCode={}, apiHost={}, protocol={}, timeout={}s",
            modelCode,
            apiHost != null ? "已配置" : "使用默认",
            protocol,
            timeout / 1000);

        return ChatModelFactory.create(modelCode, apiHost, apiKey, protocol, timeout);
    }

    /**
     * 创建通知器
     */
    private INotification createNotification() {
        String channel = config.getNotification().getChannel();

        switch (channel) {
            case "feishu":
                return new FeiShuNotification(
                        config.getNotification().getFeishuAppId(),
                        config.getNotification().getFeishuAppSecret(),
                        config.getNotification().getFeishuChatId()
                );
            case "dingtalk":
                return new DingTalkNotification(config.getNotification().getDingtalkWebhook());
            case "wecom":
                return new WeComNotification(config.getNotification().getWecomWebhook());
            default:
                throw new IllegalArgumentException("不支持的通知渠道: " + channel);
        }
    }

    /**
     * 主入口 - 从环境变量读取配置
     */
    public static void main(String[] args) {
        try {
            CodeReviewConfig config = CodeReviewConfigBuilder.fromEnv();
            CodeReviewRunner runner = new CodeReviewRunner(config);
            runner.run();
            logger.info("代码审查完成!");
        } catch (Exception e) {
            logger.error("代码审查失败", e);
            System.exit(1);
        }
    }
}