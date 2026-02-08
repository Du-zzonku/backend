package com.test.dosa_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.chat")
public class ChatPromptProperties {

    private String rootSystemPrompt = "당신은 과학/공학 학습용 3D 뷰어 서비스의 AI 튜터입니다.";
    private Map<String, String> modelSystemPrompts = new HashMap<>();
    private List<String> modelIdPaths = new ArrayList<>(List.of(
            "model.modelId",
            "model.model_id"
    ));

    public String getRootSystemPrompt() {
        return rootSystemPrompt;
    }

    public void setRootSystemPrompt(String rootSystemPrompt) {
        this.rootSystemPrompt = rootSystemPrompt;
    }

    public Map<String, String> getModelSystemPrompts() {
        return modelSystemPrompts;
    }

    public void setModelSystemPrompts(Map<String, String> modelSystemPrompts) {
        this.modelSystemPrompts = (modelSystemPrompts == null) ? new HashMap<>() : modelSystemPrompts;
    }

    public List<String> getModelIdPaths() {
        return modelIdPaths;
    }

    public void setModelIdPaths(List<String> modelIdPaths) {
        this.modelIdPaths = (modelIdPaths == null) ? new ArrayList<>() : modelIdPaths;
    }

    public String findModelSystemPrompt(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return "";
        }
        if (modelSystemPrompts == null || modelSystemPrompts.isEmpty()) {
            return "";
        }
        String key = normalizeKey(modelId);
        for (Map.Entry<String, String> e : modelSystemPrompts.entrySet()) {
            if (normalizeKey(e.getKey()).equals(key)) {
                return (e.getValue() == null) ? "" : e.getValue().trim();
            }
        }
        return "";
    }

    public String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
