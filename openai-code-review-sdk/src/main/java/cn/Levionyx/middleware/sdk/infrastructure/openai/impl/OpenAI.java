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

    private final String apiHost;
    private final String apiKey;

    public OpenAI(String apiKey) {
        this.apiHost = DEFAULT_API_HOST;
        this.apiKey = apiKey;
    }

    public OpenAI(String apiHost, String apiKey) {
        this.apiHost = apiHost;
        this.apiKey = apiKey;
    }

    @Override
    public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception {
        URL url = new URL(apiHost);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = JSON.toJSONString(requestDTO).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        connection.disconnect();

        return JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
    }
}