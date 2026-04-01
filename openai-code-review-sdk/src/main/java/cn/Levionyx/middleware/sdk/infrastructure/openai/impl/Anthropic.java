package cn.Levionyx.middleware.sdk.infrastructure.openai.impl;

import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.AnthropicRequestDTO;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.AnthropicResponseDTO;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import com.alibaba.fastjson2.JSON;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic Claude 模型实现
 * API 文档: https://docs.anthropic.com/claude/reference/messages_post
 *
 * 注意：Anthropic API 使用不同的认证方式和请求格式：
 * - 认证 Header: x-api-key (不是 Authorization: Bearer)
 * - 需要 anthropic-version Header
 * - 端点: /v1/messages (不是 /v1/chat/completions)
 */
public class Anthropic extends AbstractOpenAI {

    private static final String DEFAULT_API_HOST = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MESSAGES_PATH = "/v1/messages";

    public Anthropic(String apiKey) {
        super(DEFAULT_API_HOST, apiKey);
    }

    public Anthropic(String apiHost, String apiKey) {
        super(DEFAULT_API_HOST, apiHost, apiKey, MESSAGES_PATH);
    }

    @Override
    protected String getApiName() {
        return "Anthropic";
    }

    @Override
    public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO openAIRequest) throws Exception {
        AnthropicRequestDTO anthropicRequest = convertRequest(openAIRequest);

        URL url = new URL(apiHost);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            setupConnection(connection);

            String requestBody = JSON.toJSONString(anthropicRequest);
            logger.debug("Anthropic API 请求: host={}", apiHost);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                String errorResponse = readErrorResponse(connection);
                logger.error("Anthropic API 错误响应 [{}]: {}", responseCode, errorResponse);
                throw new RuntimeException("Anthropic API 调用失败 [" + responseCode + "]: " + errorResponse);
            }

            return readSuccessResponse(connection);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 设置 HTTP 连接参数
     * Anthropic 使用特殊的认证 Header
     */
    @Override
    protected void setupConnection(HttpURLConnection connection) throws Exception {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("x-api-key", apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);
        connection.setDoOutput(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
    }

    /**
     * 读取成功响应并转换为 OpenAI 格式
     */
    @Override
    protected ChatCompletionSyncResponseDTO readSuccessResponse(HttpURLConnection connection) throws Exception {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            AnthropicResponseDTO anthropicResponse = JSON.parseObject(content.toString(), AnthropicResponseDTO.class);
            return convertResponse(anthropicResponse);
        }
    }

    /**
     * 将 OpenAI 格式请求转换为 Anthropic 格式
     */
    private AnthropicRequestDTO convertRequest(ChatCompletionRequestDTO openAIRequest) {
        AnthropicRequestDTO anthropicRequest = new AnthropicRequestDTO();
        anthropicRequest.setModel(openAIRequest.getModel());
        anthropicRequest.setMax_tokens(4096);

        List<AnthropicRequestDTO.Message> anthropicMessages = new ArrayList<>();
        String systemPrompt = null;

        if (openAIRequest.getMessages() != null) {
            for (ChatCompletionRequestDTO.Prompt prompt : openAIRequest.getMessages()) {
                if ("system".equals(prompt.getRole())) {
                    systemPrompt = prompt.getContent();
                } else {
                    anthropicMessages.add(new AnthropicRequestDTO.Message(prompt.getRole(), prompt.getContent()));
                }
            }
        }

        anthropicRequest.setSystem(systemPrompt);
        anthropicRequest.setMessages(anthropicMessages);

        return anthropicRequest;
    }

    /**
     * 将 Anthropic 格式响应转换为 OpenAI 格式
     */
    private ChatCompletionSyncResponseDTO convertResponse(AnthropicResponseDTO anthropicResponse) {
        ChatCompletionSyncResponseDTO openAIResponse = new ChatCompletionSyncResponseDTO();

        List<ChatCompletionSyncResponseDTO.Choice> choices = new ArrayList<>();
        ChatCompletionSyncResponseDTO.Choice choice = new ChatCompletionSyncResponseDTO.Choice();
        choice.setIndex(0);
        choice.setFinish_reason(anthropicResponse.getStop_reason());

        ChatCompletionSyncResponseDTO.Message message = new ChatCompletionSyncResponseDTO.Message();
        message.setRole("assistant");
        message.setContent(anthropicResponse.getTextContent());
        choice.setMessage(message);

        choices.add(choice);
        openAIResponse.setChoices(choices);

        if (anthropicResponse.getUsage() != null) {
            ChatCompletionSyncResponseDTO.Usage usage = new ChatCompletionSyncResponseDTO.Usage();
            usage.setPrompt_tokens(anthropicResponse.getUsage().getInput_tokens());
            usage.setCompletion_tokens(anthropicResponse.getUsage().getOutput_tokens());
            usage.setTotal_tokens(usage.getPrompt_tokens() + usage.getCompletion_tokens());
            openAIResponse.setUsage(usage);
        }

        return openAIResponse;
    }
}