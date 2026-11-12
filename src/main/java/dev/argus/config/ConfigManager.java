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

    // Helper getters for Secrets & Integration
    public String getGroqApiKey() {
        String key = secrets.getString("ai.groq-api-key", "");
        if (key.isBlank()) {
            key = secrets.getString("ai.groq.api-key", "");
        }
        if (key.isBlank()) {
            key = secrets.getString("ai.groq", "");
        }
        return key.trim();
    }

    public String getOpenRouterApiKey() {
        String key = secrets.getString("ai.openrouter-api-key", "");
        if (key.isBlank()) {
            key = secrets.getString("ai.openrouter.api-key", "");
        }
        return key.trim();
    }

    public String getGeminiApiKey() {
        String key = secrets.getString("ai.gemini-api-key", "");
        if (key.isBlank()) {
            key = secrets.getString("ai.gemini.api-key", "");
        }
        return key.trim();
    }

    public String getDiscordBotToken() {
        String token = secrets.getString("discord.bot-token", "");
        if (token.isBlank()) {
            token = secrets.getString("discord.bot_token", "");
        }
        if (token.isBlank()) {
            token = secrets.getString("discord.token", "");
        }
        if (token.isBlank()) {
            token = discord.getString("bot.token", "");
        }
        return token.trim();
    }

    public String getDiscordGuildId() {
        String guildId = discord.getString("bot.guild-id", "");
        if (guildId.isBlank() || guildId.equals("000000000000000000")) {
            guildId = discord.getString("bot.guild_id", "");
        }
        if (guildId.isBlank() || guildId.equals("000000000000000000")) {
            guildId = secrets.getString("discord.guild-id", "");
        }
        if (guildId.isBlank() || guildId.equals("000000000000000000")) {
            guildId = secrets.getString("discord.guild_id", "");
        }
        return (guildId != null && !guildId.equals("000000000000000000")) ? guildId.trim() : "";
    }

    public boolean hasAnyAIKey() {
        return !getGroqApiKey().isBlank() || !getOpenRouterApiKey().isBlank() || !getGeminiApiKey().isBlank();
    }
}
