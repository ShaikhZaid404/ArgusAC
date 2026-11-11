package dev.argus.config;

import dev.argus.ArgusPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Manages all configuration domains across config.yml, messages.yml, discord.yml, and secrets.yml.
 * Supports zero-downtime hot reloading.
 */
public final class ConfigManager {

    private final ArgusPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration discord;
    private FileConfiguration secrets;

    public ConfigManager(ArgusPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads or reloads all configuration files from disk into memory.
     */
    public synchronized void reloadAll() {
        this.config = loadYaml("config.yml");
        this.messages = loadYaml("messages.yml");
        this.discord = loadYaml("discord.yml");
        this.secrets = loadYaml("secrets.yml");

        // Sync efficiency tier with prompt manager
        String configuredTier = config.getString("ai.efficiency-profile", "HIGH");
        plugin.getPromptManager().setActiveTier(configuredTier);
    }

    private FileConfiguration loadYaml(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                try {
                    file.createNewFile();
                } catch (Exception ignored) {
                }
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getDiscord() {
        return discord;
    }

    public FileConfiguration getSecrets() {
        return secrets;
    }

    // Helper getters for Secrets
    public String getGroqApiKey() {
        return secrets.getString("ai.groq-api-key", "");
    }

    public String getOpenRouterApiKey() {
        return secrets.getString("ai.openrouter-api-key", "");
    }

    public String getGeminiApiKey() {
        return secrets.getString("ai.gemini-api-key", "");
    }

    public String getDiscordBotToken() {
        return secrets.getString("discord.bot-token", "");
    }

    public boolean hasAnyAIKey() {
        return !getGroqApiKey().isBlank() || !getOpenRouterApiKey().isBlank() || !getGeminiApiKey().isBlank();
    }
}
