package com.example.aiwerewolf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private String provider = "mock";
    private ChatProvider openAiCompatible = defaults(
            "https://api.openai.com/v1",
            "gpt-4o-mini");
    private ChatProvider bailian = defaults(
            "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "qwen-plus");
    private ChatProvider deepseek = defaults(
            "https://api.deepseek.com",
            "deepseek-chat");
    private ChatProvider zhipu = defaults(
            "https://open.bigmodel.cn/api/paas/v4",
            "glm-4-flash");

    public String getProvider() {
        return provider;
    }

    public void setProvider(@Nullable String provider) {
        this.provider = blankToDefault(provider, "mock");
    }

    public ChatProvider getOpenAiCompatible() {
        return openAiCompatible;
    }

    public void setOpenAiCompatible(@Nullable ChatProvider openAiCompatible) {
        this.openAiCompatible = openAiCompatible == null
                ? defaults("https://api.openai.com/v1", "gpt-4o-mini")
                : openAiCompatible;
    }

    public ChatProvider getBailian() {
        return bailian;
    }

    public void setBailian(@Nullable ChatProvider bailian) {
        this.bailian = bailian == null
                ? defaults("https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus")
                : bailian;
    }

    public ChatProvider getDeepseek() {
        return deepseek;
    }

    public void setDeepseek(@Nullable ChatProvider deepseek) {
        this.deepseek = deepseek == null
                ? defaults("https://api.deepseek.com", "deepseek-chat")
                : deepseek;
    }

    public ChatProvider getZhipu() {
        return zhipu;
    }

    public void setZhipu(@Nullable ChatProvider zhipu) {
        this.zhipu = zhipu == null
                ? defaults("https://open.bigmodel.cn/api/paas/v4", "glm-4-flash")
                : zhipu;
    }

    public static class ChatProvider {
        private String apiKey = "";
        private String baseUrl = "";
        private String model = "";
        private int timeoutSeconds = 30;

        public ChatProvider() {
        }

        private ChatProvider(String baseUrl, String model) {
            this.baseUrl = baseUrl;
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(@Nullable String apiKey) {
            this.apiKey = apiKey == null ? "" : apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(@Nullable String baseUrl) {
            this.baseUrl = baseUrl == null ? "" : baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(@Nullable String model) {
            this.model = model == null ? "" : model;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = Math.max(1, timeoutSeconds);
        }
    }

    private static ChatProvider defaults(String baseUrl, String model) {
        return new ChatProvider(baseUrl, model);
    }

    private static String blankToDefault(@Nullable String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
