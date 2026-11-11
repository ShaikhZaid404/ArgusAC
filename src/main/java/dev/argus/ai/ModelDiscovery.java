package dev.argus.ai;

import dev.argus.ArgusPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autonomous dynamic model discovery engine.
 * Queries /v1/models every 24 hours to automatically adapt to provider catalog changes.
 */
public final class ModelDiscovery {

    private final ArgusPlugin plugin;
    private final HttpClient httpClient;
    private final Map<AIProvider, String> electedModels = new ConcurrentHashMap<>();

    public ModelDiscovery(ArgusPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    /**
     * Executes asynchronous discovery scan across all configured API keys.
     */
    public void scanAvailableModels() {
        plugin.getScheduler().runAsync(() -> {
            plugin.getLogger().info("[ArgusAC] Starting autonomous AI model discovery scan...");

            // 1. Scan Groq
            String groqKey = plugin.getConfigManager().getGroqApiKey();
            if (!groqKey.isBlank()) {
                scanOpenAICompatible(AIProvider.GROQ, groqKey, "llama-3.3-70b-versatile");
            }

            // 2. Scan OpenRouter
            String openRouterKey = plugin.getConfigManager().getOpenRouterApiKey();
            if (!openRouterKey.isBlank()) {
                scanOpenAICompatible(AIProvider.OPENROUTER, openRouterKey, "meta-llama/llama-3.3-70b-instruct:free");
            }

            // 3. Scan Gemini
            String geminiKey = plugin.getConfigManager().getGeminiApiKey();
            if (!geminiKey.isBlank()) {
                scanGemini(geminiKey);
            }

            plugin.getLogger().info("[ArgusAC] Model discovery complete. Active elected models: " + electedModels);
        });
    }

    private void scanOpenAICompatible(AIProvider provider, String apiKey, String preferredFallback) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(provider.getModelsEndpoint()))
                    .timeout(Duration.ofSeconds(6))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                String elected = extractModelFromResponse(response.body(), preferredFallback);
                electedModels.put(provider, elected);
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().fine("[ArgusAC] " + provider.getDisplayName() + " scan warning: " + e.getMessage());
        }

        // Default resilient fallback model
        electedModels.put(provider, preferredFallback);
    }

    private void scanGemini(String apiKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AIProvider.GEMINI.getModelsEndpoint() + "?key=" + apiKey))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                if (response.body().contains("gemini-1.5-flash")) {
                    electedModels.put(AIProvider.GEMINI, "gemini-1.5-flash");
                    return;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().fine("[ArgusAC] Gemini scan warning: " + e.getMessage());
        }

        electedModels.put(AIProvider.GEMINI, "gemini-1.5-flash");
    }

    private String extractModelFromResponse(String jsonBody, String preferredFallback) {
        // Fast regex extraction to avoid heavy JSON dependencies
        Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(jsonBody);

        String firstModel = null;
        while (matcher.find()) {
            String id = matcher.group(1);
            if (firstModel == null) {
                firstModel = id;
            }
            if (id.equalsIgnoreCase(preferredFallback) || id.contains("70b") || id.contains("flash")) {
                return id;
            }
        }
        return firstModel != null ? firstModel : preferredFallback;
    }

    public String getElectedModel(AIProvider provider) {
        return electedModels.getOrDefault(provider, "auto");
    }

    public Map<AIProvider, String> getElectedModels() {
        return electedModels;
    }
}
