package cn.Levionyx.middleware.sdk.infrastructure.notification.impl;

import cn.Levionyx.middleware.sdk.infrastructure.notification.INotification;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 飞书通知实现
 */
public class FeiShuNotification implements INotification {

    private static final Logger logger = LoggerFactory.getLogger(FeiShuNotification.class);

    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages";
    private static final int MARKDOWN_MAX_LENGTH = 2500;

    private final String appId;
    private final String appSecret;
    private final String chatId;

    public FeiShuNotification(String appId, String appSecret, String chatId) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.chatId = chatId;
    }

    @Override
    public void send(String logUrl, String project, String branch, String author, String message, String commitUrl, String reviewContent) throws Exception {
        String accessToken = getAccessToken();

        HttpURLConnection conn = null;
        try {
            URL url = new URL(MESSAGE_URL + "?receive_id_type=chat_id");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("receive_id", chatId);
            body.put("msg_type", "interactive");

            JSONObject card = buildCard(logUrl, project, branch, author, message, commitUrl, reviewContent);
            body.put("content", card.toJSONString());

            logger.debug("发送飞书消息, chat_id: {}, 内容长度: {}", chatId, reviewContent.length());

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

    @Override
    public String getChannelName() {
        return "feishu";
    }

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

    private JSONObject buildCard(String logUrl, String project, String branch, String author, String message, String commitUrl, String reviewContent) {
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

        // body.elements
        List<JSONObject> elements = new ArrayList<>();
        elements.add(createMarkdownElement("**项目:** " + sanitize(project)));
        elements.add(createMarkdownElement("**分支:** " + sanitize(branch)));
        elements.add(createMarkdownElement("**作者:** " + sanitize(author)));
        // 提交信息：如果有 commitUrl，显示为可点击的超链接
        if (commitUrl != null && !commitUrl.isEmpty()) {
            elements.add(createMarkdownElement("**提交:** [" + sanitize(message) + "](" + commitUrl + ")"));
        } else {
            elements.add(createMarkdownElement("**提交:** " + sanitize(message)));
        }
        elements.add(createMarkdownElement("---"));

        String sanitizedContent = sanitize(reviewContent);
        addSplitMarkdownElements(elements, sanitizedContent);

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
        List<JSONObject> behaviors = new ArrayList<>();
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

        return card;
    }

    private JSONObject createMarkdownElement(String content) {
        JSONObject element = new JSONObject();
        element.put("tag", "markdown");
        element.put("content", content);
        element.put("text_align", "left");
        element.put("text_size", "normal_v2");
        element.put("margin", "0px 0px 0px 0px");
        return element;
    }

    private void addSplitMarkdownElements(List<JSONObject> elements, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }

        if (content.length() <= MARKDOWN_MAX_LENGTH) {
            elements.add(createMarkdownElement(content));
            return;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + MARKDOWN_MAX_LENGTH, content.length());

            if (end < content.length()) {
                int lastNewline = content.lastIndexOf('\n', end);
                if (lastNewline > start) {
                    end = lastNewline + 1;
                }
            }

            String chunk = content.substring(start, end);
            elements.add(createMarkdownElement(chunk));
            start = end;
        }
    }

    private String sanitize(String text) {
        if (text == null) return "";
        return text.replaceAll("[<>]", "");
    }

    private String readStream(InputStream is) {
        if (is == null) return "";
        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            logger.error("读取流失败", e);
            return "";
        }
    }
}