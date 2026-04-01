package cn.Levionyx.middleware.sdk.infrastructure.openai.impl;

import cn.Levionyx.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.Levionyx.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import com.alibaba.fastjson2.JSON;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * OpenAI 模型实现
 * API 文档: https://platform.openai.com/docs/api-reference/chat
 */
public class OpenAI implements IOpenAI {

    private static final String DEFAULT_API_HOST = "https://api.openai.com/v1/chat/completions";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final String apiHost;
    private final String apiKey;

    public OpenAI(String apiKey) {
        this.apiHost = DEFAULT_API_HOST;
        this.apiKey = apiKey;
    }

    public OpenAI(String apiHost, String apiKey) {
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
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = JSON.toJSONString(requestDTO).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
            }

            return JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
        } finally {
            connection.disconnect();
        }
    }
}