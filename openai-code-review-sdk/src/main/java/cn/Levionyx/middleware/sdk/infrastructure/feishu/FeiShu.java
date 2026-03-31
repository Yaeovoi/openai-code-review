package cn.Levionyx.middleware.sdk.infrastructure.feishu;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 飞书开放平台 API 推送实现
 */
public class FeiShu {

    private static final Logger logger = LoggerFactory.getLogger(FeiShu.class);

    // 飞书 API 地址
    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages";

    private final String appId;
    private final String appSecret;
    private final String chatId;

    public FeiShu(String appId, String appSecret, String chatId) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.chatId = chatId;
    }

    /**
     * 获取 tenant_access_token
     */
    private String getAccessToken() throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(TOKEN_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("app_secret", appSecret);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String response;
            if (responseCode == 200) {
                response = readStream(conn.getInputStream());
            } else {
                response = readStream(conn.getErrorStream());
                throw new RuntimeException("获取飞书 access_token 失败, HTTP " + responseCode + ", 响应: " + response);
            }

            JSONObject json = JSON.parseObject(response);
            if (json.getInteger("code") == 0) {
                return json.getString("tenant_access_token");
            }

            throw new RuntimeException("获取飞书 access_token 失败: " + response);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 发送消息到群聊
     * 参考: https://open.feishu.cn/document/uAjLw4CM/ukzMukzMukzM/feishu-cards/quick-start/develop-a-card-interactive-bot
     * 应用 API 格式: content 字段直接是卡片 JSON 字符串，不需要 card 包装
     */
    public void sendMessage(String logUrl, String project, String branch, String author, String message) throws Exception {
        String accessToken = getAccessToken();

        HttpURLConnection conn = null;
        try {
            URL url = new URL(MESSAGE_URL + "?receive_id_type=chat_id");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setDoOutput(true);

            // 构建请求体 - 按照飞书应用发送消息 API 格式
            JSONObject body = new JSONObject();
            body.put("receive_id", chatId);
            body.put("msg_type", "interactive");

            // 构建卡片 JSON（直接作为 content，不需要 card 包装）
            JSONObject card = new JSONObject();
            card.put("schema", "2.0");

            // config
            JSONObject config = new JSONObject();
            config.put("update_multi", true);
            JSONObject style = new JSONObject();
            JSONObject textSize = new JSONObject();
            JSONObject normalV2 = new JSONObject();
            normalV2.put("default", "normal");
            normalV2.put("pc", "normal");
            normalV2.put("mobile", "heading");
            textSize.put("normal_v2", normalV2);
            style.put("text_size", textSize);
            config.put("style", style);
            card.put("config", config);

            // header
            JSONObject header = new JSONObject();
            JSONObject title = new JSONObject();
            title.put("tag", "plain_text");
            title.put("content", "代码审查通知");
            header.put("title", title);
            header.put("template", "blue");
            header.put("padding", "12px 12px 12px 12px");
            card.put("header", header);

            // body.elements - 显示完整的代码审查意见
            java.util.List<JSONObject> elements = new java.util.ArrayList<>();
            elements.add(createMarkdownElement("**项目:** " + sanitize(project)));
            elements.add(createMarkdownElement("**分支:** " + sanitize(branch)));
            elements.add(createMarkdownElement("**作者:** " + sanitize(author)));
            elements.add(createMarkdownElement("---"));  // 分隔线
            elements.add(createMarkdownElement("### 审查意见"));
            elements.add(createMarkdownElement(sanitize(message)));  // 显示完整的审查内容

            // button
            JSONObject button = new JSONObject();
            button.put("tag", "button");
            JSONObject buttonText = new JSONObject();
            buttonText.put("tag", "plain_text");
            buttonText.put("content", "查看审查详情");
            button.put("text", buttonText);
            button.put("type", "primary");
            button.put("width", "default");
            button.put("size", "medium");
            button.put("margin", "0px 0px 0px 0px");
            java.util.List<JSONObject> behaviors = new java.util.ArrayList<>();
            JSONObject openUrl = new JSONObject();
            openUrl.put("type", "open_url");
            openUrl.put("default_url", logUrl);
            behaviors.add(openUrl);
            button.put("behaviors", behaviors);
            elements.add(button);

            JSONObject cardBody = new JSONObject();
            cardBody.put("direction", "vertical");
            cardBody.put("padding", "12px 12px 12px 12px");
            cardBody.put("elements", elements);
            card.put("body", cardBody);

            // 应用 API: content 直接是卡片 JSON 字符串
            body.put("content", card.toJSONString());

            logger.info("发送飞书消息, 请求体: {}", body.toJSONString());

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String response;
            if (responseCode == 200) {
                response = readStream(conn.getInputStream());
            } else {
                response = readStream(conn.getErrorStream());
                logger.error("飞书消息推送失败, HTTP {}, 响应: {}", responseCode, response);
                throw new RuntimeException("飞书消息推送失败, HTTP " + responseCode + ", 响应: " + response);
            }

            JSONObject json = JSON.parseObject(response);
            if (json.getInteger("code") == 0) {
                logger.info("飞书消息推送成功!");
            } else {
                logger.error("飞书消息推送失败: {}", response);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 创建 markdown 格式的元素（完整格式）
     */
    private JSONObject createMarkdownElement(String content) {
        JSONObject element = new JSONObject();
        element.put("tag", "markdown");
        element.put("content", content);
        element.put("text_align", "left");
        element.put("text_size", "normal_v2");
        element.put("margin", "0px 0px 0px 0px");
        return element;
    }

    /**
     * 读取输入流内容
     */
    private String readStream(InputStream is) {
        if (is == null) return "";
        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            logger.error("读取流失败", e);
            return "";
        }
    }

    private JSONObject createFieldElement(String label, String value) {
        JSONObject element = new JSONObject();
        element.put("tag", "div");
        JSONObject text = new JSONObject();
        text.put("tag", "plain_text");
        text.put("content", label + ": " + sanitize(value));
        element.put("text", text);
        return element;
    }

    /**
     * 清理文本中的特殊字符，只保留基本内容
     */
    private String sanitize(String text) {
        if (text == null) return "";
        // 移除可能导致问题的特殊字符，只保留基本字符
        return text.replaceAll("[<>]", "");
    }

    /**
     * 对 URL 进行编码，处理空格和特殊字符
     */
    private String encodeUrl(String url) {
        if (url == null) return "";
        try {
            // 只编码 URL 中的文件名部分（路径最后一段）
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash > 0) {
                String baseUrl = url.substring(0, lastSlash + 1);
                String fileName = url.substring(lastSlash + 1);
                return baseUrl + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
            }
            return url;
        } catch (Exception e) {
            logger.error("URL 编码失败", e);
            return url;
        }
    }

    private JSONObject createTextContent(String text) {
        JSONObject result = new JSONObject();
        result.put("tag", "plain_text");
        result.put("content", text);
        return result;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }
}