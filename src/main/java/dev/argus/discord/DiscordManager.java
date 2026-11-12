package dev.argus.discord;

import dev.argus.ArgusPlugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

/**
 * Enterprise Discord JDA Manager.
 * Handles remote slash commands, action buttons on alert embeds, and instant Guild registration.
 */
public final class DiscordManager {

    private final ArgusPlugin plugin;
    private volatile JDA jda;

    public DiscordManager(ArgusPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Asynchronously connects the Discord bot and registers slash commands.
     */
    public void start() {
        if (!plugin.getConfigManager().getDiscord().getBoolean("enabled", true)) {
            plugin.getLogger().info("[ArgusAC] Discord integration is disabled in discord.yml.");
            return;
        }

        String token = plugin.getConfigManager().getDiscordBotToken();
        if (token.isBlank()) {
            plugin.getLogger().info("[ArgusAC] Discord Bot token is blank in secrets.yml. Integration is dormant.");
            return;
        }

        plugin.getScheduler().runAsync(() -> {
            try {
                plugin.getLogger().info("[ArgusAC] Connecting to Discord Gateway via JDA 5.2.1...");

                this.jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                        .addEventListeners(new DiscordEventListener(plugin, this))
                        .build();

                this.jda.awaitReady();
                registerSlashCommands();

                plugin.getLogger().info("[ArgusAC] Discord Bot connected successfully as: " + jda.getSelfUser().getAsTag());
            } catch (Exception e) {
                plugin.getLogger().warning("[ArgusAC] Failed to initialize Discord Bot: " + e.getMessage());
            }
        });
    }

    private void registerSlashCommands() {
        if (jda == null) return;

        SlashCommandData masterCommand = Commands.slash("argus", "Argus Master Administrative Control")
                .addSubcommands(
                        new SubcommandData("status", "View live anti-cheat engine diagnostics and server status"),
                        new SubcommandData("lookup", "Inspect suspect alt-accounts and violation history")
                                .addOption(OptionType.STRING, "player", "Minecraft username to audit", true),
                        new SubcommandData("freeze", "Remotely freeze a player on the Minecraft server")
                                .addOption(OptionType.STRING, "player", "Minecraft username to freeze", true),
                        new SubcommandData("unfreeze", "Remotely unfreeze a player")
                                .addOption(OptionType.STRING, "player", "Minecraft username to unfreeze", true)
                );

        String guildId = plugin.getConfigManager().getDiscordGuildId();
        if (!guildId.isBlank()) {
            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands().addCommands(masterCommand).queue(
                        success -> plugin.getLogger().info("[ArgusAC] Registered INSTANT Discord Slash Commands on Guild: " + guild.getName() + " (" + guildId + ")!"),
                        failure -> plugin.getLogger().warning("[ArgusAC] Failed to register Guild Slash Commands: " + failure.getMessage())
                );
                return;
            } else {
                plugin.getLogger().warning("[ArgusAC] Specified Discord Guild ID (" + guildId + ") was not found by the bot. Registering globally...");
            }
        }

        // Global fallback (takes up to 1 hour on Discord)
        jda.updateCommands().addCommands(masterCommand).queue(
                success -> plugin.getLogger().info("[ArgusAC] Discord Slash Commands registered globally."),
                failure -> plugin.getLogger().warning("[ArgusAC] Global Slash Command registration warning: " + failure.getMessage())
        );
    }

    /**
     * Dispatches an interactive alert embed with action buttons to the staff alerts channel.
     */
    public void sendAlert(String playerName, String checkName, String details, double vl, String category) {
        if (jda == null || jda.getStatus() != JDA.Status.CONNECTED) return;

        String channelId = plugin.getConfigManager().getDiscord().getString("bot.channels.alerts", "");
        if (channelId.isBlank() || channelId.equals("000000000000000000")) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ Telemetric Violation Alert • " + checkName)
                .setColor(Color.decode("#E74C3C"))
                .setThumbnail("https://mc-heads.net/avatar/" + playerName + "/128")
                .addField("Suspect Player", "`" + playerName + "`", true)
                .addField("Detection Module", "`" + checkName + " (" + category + ")`", true)
                .addField("Violation Level (VL)", "`" + String.format("%.1f", vl) + "`", true)
                .addField("Kinematic Evidence", "```\n" + details + "\n```", false)
                .addField("AI Engine Evaluation", "`" + plugin.getAiEngine().getActiveProviderName() + " (" + plugin.getAiEngine().getActiveModelName() + ")`", false)
                .setFooter("ArgusAC Enterprise • All-Seeing Guardian")
                .setTimestamp(Instant.now());

        channel.sendMessageEmbeds(embed.build())
                .setActionRow(
                        Button.danger("argus:ban:" + playerName, "Ban"),
                        Button.primary("argus:kick:" + playerName, "Kick"),
                        Button.secondary("argus:freeze:" + playerName, "Freeze"),
                        Button.secondary("argus:lookup:" + playerName, "Lookup"),
                        Button.danger("argus:dismiss", "Dismiss")
                )
                .queue(null, err -> plugin.getLogger().fine("[ArgusAC] Discord alert failed: " + err.getMessage()));
    }

    /**
     * Dispatches alt-account detection audits to the designated Discord channel.
     */
    public void sendAltAudit(String primary, String alt, String action) {
        if (jda == null || jda.getStatus() != JDA.Status.CONNECTED) return;

        String channelId = plugin.getConfigManager().getDiscord().getString("bot.channels.alt-audits", "");
        if (channelId.isBlank() || channelId.equals("000000000000000000")) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👥 Relational Alt Account Detected")
                .setColor(Color.decode("#E67E22"))
                .addField("Primary Account", "`" + primary + "`", true)
                .addField("Detected Secondary", "`" + alt + "`", true)
                .addField("Enforced Action", "`" + action + "`", true)
                .setFooter("ArgusAC Alt Intelligence")
                .setTimestamp(Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue(null, err -> {});
    }

    public void reload() {
        shutdown();
        start();
    }

    public void shutdown() {
        if (jda != null) {
            try {
                jda.shutdownNow();
            } catch (Exception ignored) {
            }
            jda = null;
        }
    }

    public JDA getJda() {
        return jda;
    }
}
