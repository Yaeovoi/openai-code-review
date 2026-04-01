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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * OpenAI API 实现的抽象基类
 * 提供通用的 HTTP 请求处理和 URL 规范化逻辑
 */
public abstract class AbstractOpenAI implements IOpenAI {

    protected static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    protected static final String PATH_SUFFIX = "chat/completions";

    /** 默认连接超时时间（毫秒） */
    protected static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    /** 默认读取超时时间（毫秒）- 3分钟 */
    protected static final int DEFAULT_READ_TIMEOUT = 180000;

    protected final String apiHost;
    protected final String apiKey;
    protected final int connectTimeout;
    protected final int readTimeout;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected AbstractOpenAI(String defaultApiHost, String apiKey) {
        this(defaultApiHost, null, apiKey, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected AbstractOpenAI(String defaultApiHost, String apiHost, String apiKey) {
        this(defaultApiHost, apiHost, apiKey, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected AbstractOpenAI(String defaultApiHost, String apiHost, String apiKey, int connectTimeout, int readTimeout) {
        this.apiHost = normalizeApiHost(apiHost, defaultApiHost, CHAT_COMPLETIONS_PATH);
        this.apiKey = apiKey;
        this.connectTimeout = connectTimeout > 0 ? connectTimeout : DEFAULT_CONNECT_TIMEOUT;
        this.readTimeout = readTimeout > 0 ? readTimeout : DEFAULT_READ_TIMEOUT;
    }

    protected AbstractOpenAI(String defaultApiHost, String apiHost, String apiKey, String apiPath, int connectTimeout, int readTimeout) {
        this.apiHost = normalizeApiHost(apiHost, defaultApiHost, apiPath);
        this.apiKey = apiKey;
        this.connectTimeout = connectTimeout > 0 ? connectTimeout : DEFAULT_CONNECT_TIMEOUT;
        this.readTimeout = readTimeout > 0 ? readTimeout : DEFAULT_READ_TIMEOUT;
    }

    /**
     * 规范化 API Host 地址
     * 使用 URI 进行安全的路径拼接，正确处理查询参数
     *
     * @param host 用户配置的 API Host
     * @param defaultHost 默认 API Host
     * @param apiPath API 路径（如 /chat/completions 或 /v1/messages）
     * @return 规范化后的完整 API 地址
     */
    protected String normalizeApiHost(String host, String defaultHost, String apiPath) {
        if (host == null || host.isEmpty()) {
            logger.warn("API_HOST 未配置，使用默认地址: {}", defaultHost);
            return defaultHost;
        }

        // 如果已经包含完整路径，直接返回
        if (host.contains(apiPath)) {
            return host;
        }

        try {
            // 使用 new URI 而非 URI.create，安全处理非法字符
            URI uri = new URI(host);
            String path = uri.getPath();

            // 处理路径拼接
            String newPath;
            if (path == null || path.isEmpty() || path.equals("/")) {
                newPath = apiPath;
            } else if (path.endsWith("/")) {
                newPath = path + apiPath.substring(1);  // 移除开头的 /
            } else {
                newPath = path + apiPath;
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
        } catch (URISyntaxException e) {
            // URI 解析失败，记录错误并返回默认地址
            logger.error("无效的 API_HOST 格式: {}, 错误: {}", host, e.getMessage());
            return defaultHost;
        } catch (Exception e) {
            logger.warn("API_HOST 解析失败，使用默认地址: {}", e.getMessage());
            return defaultHost;
        }
    }

    @Override
    public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception {
        URL url = new URL(apiHost);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            setupConnection(connection);

            String requestBody = JSON.toJSONString(requestDTO);
            logger.debug("API 请求: host={}, timeout={}s", apiHost, readTimeout / 1000);

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
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
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