package cn.Levionyx.middleware.sdk.infrastructure.notification.impl;

import cn.Levionyx.middleware.sdk.infrastructure.notification.INotification;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 钉钉通知实现 - 通过 Webhook
 */
public class DingTalkNotification implements INotification {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkNotification.class);

    private final String webhook;

    public DingTalkNotification(String webhook) {
        this.webhook = webhook;
    }

    @Override
    public void send(String logUrl, String project, String branch, String author, String reviewContent) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(webhook);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            // 构建钉钉 Markdown 消息
            JSONObject body = new JSONObject();
            body.put("msgtype", "markdown");

            JSONObject markdown = new JSONObject();
            markdown.put("title", "代码审查通知");

            StringBuilder content = new StringBuilder();
            content.append("## 代码审查通知\n\n");
            content.append("- **项目:** ").append(sanitize(project)).append("\n");
            content.append("- **分支:** ").append(sanitize(branch)).append("\n");
            content.append("- **作者:** ").append(sanitize(author)).append("\n\n");
            content.append("---\n\n");
            content.append(sanitize(reviewContent)).append("\n\n");
            content.append("[查看审查详情](").append(logUrl).append(")");

            markdown.put("text", content.toString());
            body.put("markdown", markdown);

            logger.info("发送钉钉消息, 请求体: {}", body.toJSONString());

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                logger.info("钉钉消息推送成功!");
            } else {
                logger.error("钉钉消息推送失败, HTTP {}", responseCode);
                throw new RuntimeException("钉钉消息推送失败, HTTP " + responseCode);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public String getChannelName() {
        return "dingtalk";
    }

    private String sanitize(String text) {
        if (text == null) return "";
        return text.replaceAll("[<>]", "");
    }
}