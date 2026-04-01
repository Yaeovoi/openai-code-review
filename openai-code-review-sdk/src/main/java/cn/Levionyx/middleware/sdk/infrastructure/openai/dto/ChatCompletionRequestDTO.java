package cn.Levionyx.middleware.sdk.infrastructure.openai.dto;

import cn.Levionyx.middleware.sdk.domain.model.ChatModel;

import java.util.List;

/**
 * OpenAI Chat Completions API 请求 DTO
 */
public class ChatCompletionRequestDTO {

    private String model = ChatModel.QWEN_CODER_PLUS.getCode();
    private List<Prompt> messages;

    /**
     * 提示消息
     */
    public static class Prompt {
        private String role;
        private String content;

        public Prompt() {
        }

        public Prompt(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Prompt> getMessages() {
        return messages;
    }

    public void setMessages(List<Prompt> messages) {
        this.messages = messages;
    }
}
