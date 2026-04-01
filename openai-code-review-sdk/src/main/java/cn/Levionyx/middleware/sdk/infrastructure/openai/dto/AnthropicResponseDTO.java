package cn.Levionyx.middleware.sdk.infrastructure.openai.dto;

import java.util.List;

/**
 * Anthropic Claude API 响应 DTO
 * API 文档: https://docs.anthropic.com/claude/reference/messages_post
 */
public class AnthropicResponseDTO {

    private String id;
    private String type;
    private String role;
    private List<ContentBlock> content;
    private String model;
    private String stop_reason;
    private Usage usage;

    public static class ContentBlock {
        private String type;
        private String text;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class Usage {
        private Integer input_tokens;
        private Integer output_tokens;

        public Integer getInput_tokens() {
            return input_tokens;
        }

        public void setInput_tokens(Integer input_tokens) {
            this.input_tokens = input_tokens;
        }

        public Integer getOutput_tokens() {
            return output_tokens;
        }

        public void setOutput_tokens(Integer output_tokens) {
            this.output_tokens = output_tokens;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<ContentBlock> getContent() {
        return content;
    }

    public void setContent(List<ContentBlock> content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStop_reason() {
        return stop_reason;
    }

    public void setStop_reason(String stop_reason) {
        this.stop_reason = stop_reason;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    /**
     * 获取响应文本内容
     */
    public String getTextContent() {
        if (content == null || content.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : content) {
            if ("text".equals(block.getType()) && block.getText() != null) {
                sb.append(block.getText());
            }
        }
        return sb.toString();
    }
}