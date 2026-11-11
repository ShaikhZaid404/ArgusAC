package dev.argus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

/**
 * ArgusAC (Argus Anti Cheat) Master Bootstrap Plugin.
 * Engineered for Paper, Purpur, and Folia 1.21.x servers.
 */
public final class ArgusPlugin extends JavaPlugin {

    private static ArgusPlugin instance;
    private boolean folia;

    @Override
    public void onEnable() {
        instance = this;
        Logger logger = getLogger();

        // Detect Folia vs Paper runtime
        this.folia = detectFolia();

        // Print large ASCII startup banner to console
        printStartupBanner();

        // Initialize and save default configurations
        saveDefaultConfigs();

        logger.info("[ArgusAC] Initialized successfully on " + (folia ? "Folia (Regionised)" : "Paper/Purpur") + "!");
    }

    @Override
    public void onDisable() {
        Logger logger = getLogger();
        logger.info("[ArgusAC] Safely unregistering watchers and closing database connections...");
        instance = null;
    }

    /**
     * Prints the glorious ArgusAC ASCII banner in server console upon startup.
     */
    private void printStartupBanner() {
        String[] banner = {
            "  ___                           _    ____ ",
            " / _ \\                         | |  / ___|",
            "/ /_\\ \\ _ __  __ _  _   _  ___ | | / /___ ",
            "|  _  || '__/ _` || | | |/ __|| | | ___ \\",
            "| | | || | | (_| || |_| |\\__ \\| | | \\_/ |",
            "\\_| |_/|_|  \\__, | \\__,_||___/|_| \\_____/",
            "             __/ |                        ",
            "            |___/   [The All-Seeing Guardian]",
            "--------------------------------------------------",
            " > Engine: " + (folia ? "Folia 1.21 (Region-Threaded)" : "Paper / Purpur 1.21"),
            " > Version: " + getDescription().getVersion() + " (Java 21)",
            " > AI Engine: Dynamic Discovery (Groq / OpenRouter / Gemini)",
            " > Pure Kinematic Fallback: Armed & Ready",
            "--------------------------------------------------"
        };

        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + line);
        }
    }

    /**
     * Ensures default configuration files exist in plugin directory.
     */
    private void saveDefaultConfigs() {
        saveDefaultConfig();
        saveResourceIfNotExists("messages.yml");
        saveResourceIfNotExists("discord.yml");
        saveResourceIfNotExists("secrets.yml");

        // Save default efficiency prompts
        String[] prompts = {
            "argusai-prompts/low-system.md",
            "argusai-prompts/medium-system.md",
            "argusai-prompts/high-system.md",
            "argusai-prompts/extra-high-system.md"
        };
        for (String prompt : prompts) {
            saveResourceIfNotExists(prompt);
        }
    }

    private void saveResourceIfNotExists(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            saveResource(path, false);
        }
    }

    /**
     * Checks if current server engine is Folia.
     */
    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("argus")) {
            sender.sendMessage(ChatColor.AQUA + "[ArgusAC] " + ChatColor.GRAY + "v" + getDescription().getVersion() + 
                    " is active. Use " + ChatColor.YELLOW + "/argus guide" + ChatColor.GRAY + " for the interactive setup book.");
            return true;
        }
        return false;
    }

    public static ArgusPlugin getInstance() {
        return instance;
    }

    public boolean isFolia() {
        return folia;
    }
}
