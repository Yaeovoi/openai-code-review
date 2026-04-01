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
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * OpenAI API 实现的抽象基类
 * 提供通用的 HTTP 请求处理和 URL 规范化逻辑
 */
public abstract class AbstractOpenAI implements IOpenAI {

    protected static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    protected static final String PATH_SUFFIX = "chat/completions";

    protected final String apiHost;
    protected final String apiKey;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected AbstractOpenAI(String defaultApiHost, String apiKey) {
        this.apiHost = defaultApiHost;
        this.apiKey = apiKey;
    }

    protected AbstractOpenAI(String defaultApiHost, String apiHost, String apiKey) {
        this.apiHost = normalizeApiHost(apiHost, defaultApiHost);
        this.apiKey = apiKey;
    }

    /**
     * 规范化 API Host 地址
     * 使用 URI 进行安全的路径拼接，正确处理查询参数
     *
     * @param host 用户配置的 API Host
     * @param defaultHost 默认 API Host
     * @return 规范化后的完整 API 地址
     */
    protected String normalizeApiHost(String host, String defaultHost) {
        if (host == null || host.isEmpty()) {
            logger.warn("API_HOST 未配置，使用默认地址: {}", defaultHost);
            return defaultHost;
        }

        // 如果已经包含完整路径，直接返回
        if (host.contains(CHAT_COMPLETIONS_PATH)) {
            return host;
        }

        try {
            // 使用 URI 进行安全的路径拼接
            URI uri = URI.create(host);
            String path = uri.getPath();

            // 处理路径拼接
            String newPath;
            if (path == null || path.isEmpty()) {
                newPath = CHAT_COMPLETIONS_PATH;
            } else if (path.endsWith("/")) {
                newPath = path + PATH_SUFFIX;
            } else {
                newPath = path + CHAT_COMPLETIONS_PATH;
            }

            // 重建 URI，保留查询参数
            URI normalizedUri = new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    newPath,
                    uri.getQuery(),
                    null
            );

            return normalizedUri.toString();
        } catch (Exception e) {
            // 如果 URI 解析失败，使用简单的字符串拼接
            logger.warn("API_HOST 解析失败，使用简单拼接: {}", e.getMessage());
            if (host.endsWith("/")) {
                return host + PATH_SUFFIX;
            }
            return host + CHAT_COMPLETIONS_PATH;
        }
    }

    @Override
    public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception {
        URL url = new URL(apiHost);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            setupConnection(connection);

            String requestBody = JSON.toJSONString(requestDTO);
            logger.debug("API 请求: host={}, body={}", apiHost, requestBody);

            // 发送请求
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 处理响应
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                String errorResponse = readErrorResponse(connection);
                logger.error("API 错误响应 [{}]: {}", responseCode, errorResponse);
                throw new RuntimeException(getApiName() + " API 调用失败 [" + responseCode + "]: " + errorResponse);
            }

            // 读取成功响应
            return readSuccessResponse(connection);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 设置 HTTP 连接参数
     * 子类可以覆盖此方法添加额外的请求头
     */
    protected void setupConnection(HttpURLConnection connection) throws Exception {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
    }

    /**
     * 读取错误响应
     */
    protected String readErrorResponse(HttpURLConnection connection) {
        try {
            if (connection.getErrorStream() == null) {
                return "无错误响应内容 (可能请求未到达服务器或网络问题)";
            }
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
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

    /**
     * 读取成功响应
     */
    protected ChatCompletionSyncResponseDTO readSuccessResponse(HttpURLConnection connection) throws Exception {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            return JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
        }
    }

    /**
     * 获取 API 名称，用于日志和异常信息
     */
    protected abstract String getApiName();
}