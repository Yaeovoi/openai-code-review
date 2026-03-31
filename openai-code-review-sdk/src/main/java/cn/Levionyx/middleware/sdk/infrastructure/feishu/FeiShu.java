package cn.Levionyx.middleware.sdk.infrastructure.feishu;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
        URL url = new URL(TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setDoOutput(true);

        JSONObject body = new JSONObject();
        body.put("app_id", appId);
        body.put("app_secret", appSecret);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
        }

        Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
        String response = scanner.useDelimiter("\\A").next();
        scanner.close();

        JSONObject json = JSON.parseObject(response);
        if (json.getInteger("code") == 0) {
            return json.getString("tenant_access_token");
        }

        throw new RuntimeException("获取飞书 access_token 失败: " + response);
    }

    /**
     * 发送消息到群聊
     */
    public void sendMessage(String logUrl, String project, String branch, String author, String message) throws Exception {
        String accessToken = getAccessToken();

        URL url = new URL(MESSAGE_URL + "?receive_id_type=chat_id");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setDoOutput(true);

        // 构建消息内容
        JSONObject body = new JSONObject();
        body.put("receive_id", chatId);
        body.put("msg_type", "interactive");

        // 构建卡片消息
        JSONObject card = new JSONObject();
        JSONObject config = new JSONObject();
        config.put("wide_screen_mode", true);
        card.put("config", config);

        // 标题
        JSONObject header = new JSONObject();
        JSONObject title = new JSONObject();
        title.put("content", "代码审查通知");
        title.put("tag", "plain_text");
        header.put("title", title);
        header.put("template", "blue");
        card.put("header", header);

        // 内容元素
        java.util.List<JSONObject> elements = new java.util.ArrayList<>();

        // 项目信息
        elements.add(createFieldElement("项目", project));
        elements.add(createFieldElement("分支", branch));
        elements.add(createFieldElement("作者", author));
        elements.add(createFieldElement("说明", truncate(message, 50)));

        // 查看详情链接
        JSONObject actionElement = new JSONObject();
        actionElement.put("tag", "action");
        java.util.List<JSONObject> actions = new java.util.ArrayList<>();
        JSONObject action = new JSONObject();
        action.put("tag", "button");
        action.put("text", createTextContent("查看审查详情"));
        action.put("type", "primary");
        action.put("url", logUrl);
        actions.add(action);
        actionElement.put("actions", actions);
        elements.add(actionElement);

        card.put("elements", elements);

        JSONObject content = new JSONObject();
        content.put("card", card);
        body.put("content", content.toJSONString());

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
        }

        Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
        String response = scanner.useDelimiter("\\A").next();
        scanner.close();

        JSONObject json = JSON.parseObject(response);
        if (json.getInteger("code") == 0) {
            logger.info("飞书消息推送成功!");
        } else {
            logger.error("飞书消息推送失败: {}", response);
        }
    }

    private JSONObject createFieldElement(String label, String value) {
        JSONObject element = new JSONObject();
        element.put("tag", "div");
        JSONObject field = new JSONObject();
        field.put("is_short", true);
        JSONObject text = new JSONObject();
        text.put("tag", "lark_md");
        text.put("content", "**" + label + ":** " + value);
        field.put("text", text);
        element.put("fields", java.util.Collections.singletonList(field));
        return element;
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