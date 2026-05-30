package com.example.aiwerewolf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private String provider = "mock";
    private Bailian bailian = new Bailian();

    public String getProvider() {
        return provider;
    }

    public void setProvider(@Nullable String provider) {
        this.provider = blankToDefault(provider, "mock");
    }

    public Bailian getBailian() {
        return bailian;
    }

    public void setBailian(@Nullable Bailian bailian) {
        this.bailian = bailian == null ? new Bailian() : bailian;
    }

    public static class Bailian {
        private String apiKey = "";
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String model = "qwen-plus";
        private int timeoutSeconds = 30;

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
            this.baseUrl = blankToDefault(baseUrl, "https://dashscope.aliyuncs.com/compatible-mode/v1");
        }

        public String getModel() {
            return model;
        }

        public void setModel(@Nullable String model) {
            this.model = blankToDefault(model, "qwen-plus");
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = Math.max(1, timeoutSeconds);
        }
    }

    private static String blankToDefault(@Nullable String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
