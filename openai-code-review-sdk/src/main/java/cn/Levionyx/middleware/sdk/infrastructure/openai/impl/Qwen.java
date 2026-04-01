package cn.Levionyx.middleware.sdk.infrastructure.openai.impl;

import cn.Levionyx.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 阿里云百炼 Qwen 模型实现
 * API 文档: https://help.aliyun.com/zh/model-studio/developer-reference/use-qwen-by-calling-api
 *
 * 支持两种 API 地址：
 * 1. 百炼 OpenAI 兼容 API（默认）: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
 * 2. 灵码 codingplan API: https://coding.dashscope.aliyuncs.com/v1/chat/completions
 */
public class Qwen implements IOpenAI {

    // 阿里云百炼 OpenAI 兼容 API 地址（默认）
    private static final String DEFAULT_API_HOST = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final String apiHost;
    private final String apiKey;

    private static final Logger logger = LoggerFactory.getLogger(Qwen.class);

    public Qwen(String apiKey) {
        this.apiHost = DEFAULT_API_HOST;
        this.apiKey = apiKey;
    }

    public Qwen(String apiHost, String apiKey) {
        this.apiHost = normalizeApiHost(apiHost);
        this.apiKey = apiKey;
    }

    /**
     * 规范化 API Host 地址
     * 如果地址不包含 /chat/completions，自动补全
     */
    private String normalizeApiHost(String host) {
        if (host == null || host.isEmpty()) {
            return DEFAULT_API_HOST;
        }
        if (host.endsWith(CHAT_COMPLETIONS_PATH)) {
            return host;
        }
        // 补全路径
        if (host.endsWith("/")) {
            return host + "chat/completions";
        }
        return host + CHAT_COMPLETIONS_PATH;
    }

    @Override
    public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception {
        URL url = new URL(apiHost);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String requestBody = JSON.toJSONString(requestDTO);
        logger.debug("Qwen API 请求: {}", requestBody);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            String errorResponse = readErrorResponse(connection);
            logger.error("Qwen API 错误响应 [{}]: {}", responseCode, errorResponse);
            throw new RuntimeException("Qwen API 调用失败 [" + responseCode + "]: " + errorResponse);
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            return JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
        } finally {
            connection.disconnect();
        }
    }

    private String readErrorResponse(HttpURLConnection connection) {
        try {
            if (connection.getErrorStream() == null) {
                return "无错误响应内容 (可能请求未到达服务器或网络问题)";
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                return content.toString();
            }
        } catch (Exception e) {
            return "无法读取错误响应: " + e.getMessage();
        }
    }
}