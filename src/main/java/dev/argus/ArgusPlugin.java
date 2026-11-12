package dev.argus;

import dev.argus.ai.AIEngine;
import dev.argus.alts.AltManager;
import dev.argus.checks.CheckManager;
import dev.argus.commands.ArgusCommand;
import dev.argus.config.ConfigManager;
import dev.argus.config.PromptManager;
import dev.argus.discord.DiscordManager;
import dev.argus.freeze.FreezeManager;
import dev.argus.listeners.CheckListener;
import dev.argus.listeners.PlayerConnectionListener;
import dev.argus.scheduler.ArgusScheduler;
import dev.argus.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * ArgusAC (Argus Anti Cheat) Master Bootstrap Plugin.
 * Engineered for Paper, Purpur, and Folia 1.21.x servers.
 */
public final class ArgusPlugin extends JavaPlugin {

    private static ArgusPlugin instance;
    private boolean folia;

    private ArgusScheduler scheduler;
    private PromptManager promptManager;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private AltManager altManager;
    private AIEngine aiEngine;
    private CheckManager checkManager;
    private FreezeManager freezeManager;
    private DiscordManager discordManager;

    @Override
    public void onEnable() {
        instance = this;
        Logger logger = getLogger();

        // 1. Detect Folia vs Paper runtime
        this.folia = detectFolia();

        // 2. Initialize Scheduler Abstraction
        this.scheduler = new ArgusScheduler(this);

        // 3. Initialize Memory Prompt & Config Managers
        this.promptManager = new PromptManager(this);
        this.promptManager.loadPrompts();

        this.configManager = new ConfigManager(this);
        this.configManager.reloadAll();

        // 4. Initialize SQLite Relational Database & Alt Manager
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        this.altManager = new AltManager(this);

        // 5. Initialize Multi-Provider AI Engine & Fallback Router
        this.aiEngine = new AIEngine(this);
        this.aiEngine.start();

        // 6. Initialize Detection Checks & Freeze Manager
        this.checkManager = new CheckManager(this);
        this.freezeManager = new FreezeManager(this);

        // 7. Register Event Listeners
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new CheckListener(this), this);

        // 8. Register Commands & Tab Completers
        ArgusCommand masterCommand = new ArgusCommand(this);
        if (getCommand("argus") != null) {
            getCommand("argus").setExecutor(masterCommand);
            getCommand("argus").setTabCompleter(masterCommand);
        }

        // 9. Initialize Enterprise Discord Integration
        this.discordManager = new DiscordManager(this);
        this.discordManager.start();

        // 10. Print Large Console Startup Banner
        printStartupBanner();

        logger.info("[ArgusAC] All watchers, AI engines, and database pools armed successfully on "
                + (folia ? "Folia (Regionised)" : "Paper/Purpur") + "!");
    }

    @Override
    public void onDisable() {
        Logger logger = getLogger();
        logger.info("[ArgusAC] Safely shutting down watchers and closing connection pools...");

        if (discordManager != null) {
            discordManager.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        instance = null;
    }

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
            " > Alt Protection: SQLite Relational Primary Ledger",
            "--------------------------------------------------"
        };

        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + line);
        }
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static ArgusPlugin getInstance() {
        return instance;
    }

    public boolean isFolia() {
        return folia;
    }

    public ArgusScheduler getScheduler() {
        return scheduler;
    }

    public PromptManager getPromptManager() {
        return promptManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public AltManager getAltManager() {
        return altManager;
    }

    public AIEngine getAiEngine() {
        return aiEngine;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }
}
