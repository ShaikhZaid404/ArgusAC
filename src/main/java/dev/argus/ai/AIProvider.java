package dev.argus.ai;

/**
 * Supported dynamic AI providers for real-time cheat forensic evaluation.
 */
public enum AIProvider {
    GROQ("Groq Cloud", "https://api.groq.com/openai/v1/chat/completions", "https://api.groq.com/openai/v1/models"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "https://openrouter.ai/api/v1/models"),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta/models/", "https://generativelanguage.googleapis.com/v1beta/models");

    private final String displayName;
    private final String chatEndpoint;
    private final String modelsEndpoint;

    AIProvider(String displayName, String chatEndpoint, String modelsEndpoint) {
        this.displayName = displayName;
        this.chatEndpoint = chatEndpoint;
        this.modelsEndpoint = modelsEndpoint;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getChatEndpoint() {
        return chatEndpoint;
    }

    public String getModelsEndpoint() {
        return modelsEndpoint;
    }
}
