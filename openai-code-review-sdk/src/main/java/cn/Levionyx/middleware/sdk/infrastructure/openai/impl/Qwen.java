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
 * 使用 codingplan 专用 API: https://coding.dashscope.aliyuncs.com/v1/chat/completions
 *
 * codingplan 是阿里云灵码服务，专门用于代码相关任务
 * 支持 qwen-turbo, qwen-plus, qwen-max, qwen-coder-plus 等模型
 */
public class Qwen implements IOpenAI {

    // 阿里云灵码 codingplan 专用 API 地址
    private static final String DEFAULT_API_HOST = "https://coding.dashscope.aliyuncs.com/v1/chat/completions";

    private final String apiHost;
    private final String apiKey;

    private static final Logger logger = LoggerFactory.getLogger(Qwen.class);

    public Qwen(String apiKey) {
        this.apiHost = DEFAULT_API_HOST;
        this.apiKey = apiKey;
    }

    public Qwen(String apiHost, String apiKey) {
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            return content.toString();
        } catch (Exception e) {
            return "无法读取错误响应: " + e.getMessage();
        }
    }
}