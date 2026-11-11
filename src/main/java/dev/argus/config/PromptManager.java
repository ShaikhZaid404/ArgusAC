package dev.argus.config;

import dev.argus.ArgusPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * High-performance, in-memory prompt cache manager.
 * Pre-loads all prompt markdown files on server startup to eliminate runtime disk I/O.
 */
public final class PromptManager {

    private final ArgusPlugin plugin;
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();
    private String activeTier = "HIGH";

    public PromptManager(ArgusPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Reads all prompt markdown files into memory.
     */
    public synchronized void loadPrompts() {
        promptCache.clear();
        File promptsDir = new File(plugin.getDataFolder(), "argusai-prompts");
        if (!promptsDir.exists()) {
            promptsDir.mkdirs();
        }

        String[] tiers = {"low", "medium", "high", "extra-high"};
        for (String tier : tiers) {
            String fileName = tier + "-system.md";
            File file = new File(promptsDir, fileName);
            if (!file.exists()) {
                String resPath = "argusai-prompts/" + fileName;
                if (plugin.getResource(resPath) != null) {
                    plugin.saveResource(resPath, false);
                }
            }

            try {
                if (file.exists()) {
                    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    promptCache.put(tier.toUpperCase(), content);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("[ArgusAC] Failed to load prompt file: " + fileName + " (" + e.getMessage() + ")");
            }
        }

        plugin.getLogger().info("[ArgusAC] Pre-loaded " + promptCache.size() + " AI efficiency prompts into RAM.");
    }

    /**
     * Retrieves the currently active system prompt directly from RAM cache.
     */
    public String getActivePrompt() {
        return promptCache.getOrDefault(activeTier.toUpperCase(), promptCache.getOrDefault("HIGH", ""));
    }

    /**
     * Hot-swaps the active efficiency tier in memory.
     */
    public boolean setActiveTier(String tier) {
        String normalized = tier.toUpperCase().replace("_", "-");
        if (promptCache.containsKey(normalized)) {
            this.activeTier = normalized;
            return true;
        }
        return false;
    }

    public String getActiveTier() {
        return activeTier;
    }
}
