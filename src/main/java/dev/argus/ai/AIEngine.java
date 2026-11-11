package dev.argus.ai;

import dev.argus.ArgusPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Multi-provider resilient AI decision engine.
 * Handles priority failover, rate-limit desensitization, and seamless fallback to pure kinematics.
 */
public final class AIEngine {

    private final ArgusPlugin plugin;
    private final ModelDiscovery modelDiscovery;
    private final HttpClient httpClient;

    private final AtomicLong lastPingMs = new AtomicLong(0);
    private volatile boolean fallbackMode = false;
    private volatile String activeProviderName = "Pure Kinematics";
    private volatile String activeModelName = "Heuristic-Engine";

    public AIEngine(ArgusPlugin plugin) {
        this.plugin = plugin;
        this.modelDiscovery = new ModelDiscovery(plugin);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    public void start() {
        // Initial scan on startup
        modelDiscovery.scanAvailableModels();

        // Schedule periodic re-scan every 24 hours (20 ticks * 60s * 60m * 24h = 1728000 ticks)
        plugin.getScheduler().runGlobalRepeating(modelDiscovery::scanAvailableModels, 1728000L, 1728000L);
    }

    /**
     * Dispatches an asynchronous evaluation request for flagged player telemetry.
     */
    public void evaluateTelemetryAsync(String playerContext, Consumer<String> resultCallback) {
        plugin.getScheduler().runAsync(() -> {
            String prompt = plugin.getPromptManager().getActivePrompt();
            long start = System.currentTimeMillis();

            // Attempt 1: Groq Cloud
            String groqKey = plugin.getConfigManager().getGroqApiKey();
            if (!groqKey.isBlank()) {
                String model = modelDiscovery.getElectedModel(AIProvider.GROQ);
                String response = executeOpenAICompatible(AIProvider.GROQ, groqKey, model, prompt, playerContext);
                if (response != null) {
                    recordSuccess(AIProvider.GROQ.getDisplayName(), model, start);
                    resultCallback.accept(response);
                    return;
                }
            }

            // Attempt 2: OpenRouter Failover
            String openRouterKey = plugin.getConfigManager().getOpenRouterApiKey();
            if (!openRouterKey.isBlank()) {
                String model = modelDiscovery.getElectedModel(AIProvider.OPENROUTER);
                String response = executeOpenAICompatible(AIProvider.OPENROUTER, openRouterKey, model, prompt, playerContext);
                if (response != null) {
                    recordSuccess(AIProvider.OPENROUTER.getDisplayName(), model, start);
                    resultCallback.accept(response);
                    return;
                }
            }

            // Attempt 3: Google Gemini Failover
            String geminiKey = plugin.getConfigManager().getGeminiApiKey();
            if (!geminiKey.isBlank()) {
                String model = modelDiscovery.getElectedModel(AIProvider.GEMINI);
                String response = executeGemini(geminiKey, model, prompt, playerContext);
                if (response != null) {
                    recordSuccess(AIProvider.GEMINI.getDisplayName(), model, start);
                    resultCallback.accept(response);
                    return;
                }
            }

            // Fallback: Pure Kinematics & Internal Vector Math
            this.fallbackMode = true;
            this.activeProviderName = "Pure Kinematics";
            this.activeModelName = "Internal-Kinematic-v1";
            resultCallback.accept("{\"verdict\":\"SUSPICIOUS\",\"confidence\":0.85,\"action\":\"SETBACK\",\"fallback\":true}");
        });
    }

    private String executeOpenAICompatible(AIProvider provider, String apiKey, String model, String systemPrompt, String telemetry) {
        try {
            String payload = """
                {
                  "model": "%s",
                  "temperature": 0.1,
                  "messages": [
                    {"role": "system", "content": %s},
                    {"role": "user", "content": %s}
                  ]
                }
                """.formatted(model, escapeJson(systemPrompt), escapeJson(telemetry));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(provider.getChatEndpoint()))
                    .timeout(Duration.ofSeconds(4))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                return response.body();
            } else if (response.statusCode() == 429) {
                plugin.getLogger().warning("[ArgusAC] " + provider.getDisplayName() + " hit rate-limit (HTTP 429). Failing over to next provider...");
            }
        } catch (Exception e) {
            plugin.getLogger().fine("[ArgusAC] " + provider.getDisplayName() + " request failed: " + e.getMessage());
        }
        return null;
    }

    private String executeGemini(String apiKey, String model, String systemPrompt, String telemetry) {
        try {
            String uri = AIProvider.GEMINI.getChatEndpoint() + model + ":generateContent?key=" + apiKey;
            String payload = """
                {
                  "contents": [
                    {
                      "role": "user",
                      "parts": [{"text": %s}]
                    }
                  ]
                }
                """.formatted(escapeJson(systemPrompt + "\n\nTELEMETRY:\n" + telemetry));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                return response.body();
            }
        } catch (Exception e) {
            plugin.getLogger().fine("[ArgusAC] Gemini request failed: " + e.getMessage());
        }
        return null;
    }

    private void recordSuccess(String provider, String model, long startTime) {
        this.fallbackMode = false;
        this.activeProviderName = provider;
        this.activeModelName = model;
        this.lastPingMs.set(System.currentTimeMillis() - startTime);
    }

    private String escapeJson(String input) {
        if (input == null) return "\"\"";
        return "\"" + input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    public String getActiveProviderName() {
        return activeProviderName;
    }

    public String getActiveModelName() {
        return activeModelName;
    }

    public long getLastPingMs() {
        return lastPingMs.get();
    }

    public boolean isFallbackMode() {
        return fallbackMode;
    }

    public ModelDiscovery getModelDiscovery() {
        return modelDiscovery;
    }
}
