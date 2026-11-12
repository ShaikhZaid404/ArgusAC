package dev.argus.discord;

import dev.argus.ArgusPlugin;
import dev.argus.alts.PlayerRecord;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

/**
 * Handles incoming Discord slash commands, verification queries, and staff alert buttons.
 */
public final class DiscordEventListener extends ListenerAdapter {

    private final ArgusPlugin plugin;
    private final DiscordManager discordManager;

    public DiscordEventListener(ArgusPlugin plugin, DiscordManager discordManager) {
        this.plugin = plugin;
        this.discordManager = discordManager;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase("argus")) {
            return;
        }

        // Validate Staff Authorization
        Member member = event.getMember();
        if (member == null || (!member.hasPermission(Permission.ADMINISTRATOR) && !hasStaffRole(member))) {
            event.reply("⛔ **Access Denied:** You do not have permission to execute ArgusAC administrative commands.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.reply("Unknown subcommand.").setEphemeral(true).queue();
            return;
        }

        switch (subcommand.toLowerCase()) {
            case "status" -> handleStatus(event);
            case "lookup" -> handleLookup(event);
            case "freeze" -> handleFreeze(event);
            case "unfreeze" -> handleUnfreeze(event);
            default -> event.reply("Unrecognized command.").setEphemeral(true).queue();
        }
    }

    private void handleStatus(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ ArgusAC Enterprise System Status")
                .setColor(Color.decode("#3498DB"))
                .addField("Server Engine", plugin.isFolia() ? "Folia (Region-Threaded)" : "Paper / Purpur 1.21", true)
                .addField("Online Players", String.valueOf(Bukkit.getOnlinePlayers().size()), true)
                .addField("Loaded Checks", plugin.getCheckManager().getChecks().size() + " Modules", true)
                .addField("Active AI Engine", plugin.getAiEngine().getActiveProviderName(), true)
                .addField("Selected Model", plugin.getAiEngine().getActiveModelName(), true)
                .addField("AI Latency", plugin.getAiEngine().getLastPingMs() + "ms", true)
                .addField("Prompt Profile", plugin.getPromptManager().getActiveTier(), true)
                .addField("Alt Ledger", "SQLite WAL Active", true)
                .setFooter("ArgusAC Enterprise • All-Seeing Guardian")
                .setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleLookup(SlashCommandInteractionEvent event) {
        String target = event.getOption("player") != null ? event.getOption("player").getAsString() : "";
        if (target.isBlank()) {
            event.reply("Please specify a player username.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        plugin.getScheduler().runAsync(() -> {
            List<PlayerRecord> records = plugin.getAltManager().getRecordsByUsername(target);
            if (records.isEmpty()) {
                event.getHook().sendMessage("🔍 No historical or linked accounts found for **" + target + "**.").queue();
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < records.size(); i++) {
                PlayerRecord rec = records.get(i);
                String tag = (i == 0) ? " `[PRIMARY]`" : " `[ALT]`";
                sb.append("• **").append(rec.username()).append("**").append(tag)
                  .append(" - UUID: `").append(rec.uuid()).append("`\n");
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📋 Suspect Account Audit: " + target)
                    .setColor(Color.decode("#2ECC71"))
                    .setDescription(sb.toString())
                    .setFooter("ArgusAC Intelligence Ledger")
                    .setTimestamp(Instant.now());

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }

    private void handleFreeze(SlashCommandInteractionEvent event) {
        String target = event.getOption("player") != null ? event.getOption("player").getAsString() : "";
        if (target.isBlank()) {
            event.reply("Please specify a target player.").setEphemeral(true).queue();
            return;
        }

        plugin.getScheduler().runGlobal(() -> {
            Player p = Bukkit.getPlayer(target);
            if (p == null) {
                event.reply("❌ Player **" + target + "** is not online on the Minecraft server.").setEphemeral(true).queue();
                return;
            }

            String code = plugin.getFreezeManager().freezePlayer(p, event.getUser().getAsTag() + " (Discord)");
            event.reply("❄️ **" + p.getName() + "** has been frozen in-game for screenshare investigation!\nVerification Code: `" + code + "`").queue();
        });
    }

    private void handleUnfreeze(SlashCommandInteractionEvent event) {
        String target = event.getOption("player") != null ? event.getOption("player").getAsString() : "";
        if (target.isBlank()) {
            event.reply("Please specify a target player.").setEphemeral(true).queue();
            return;
        }

        plugin.getScheduler().runGlobal(() -> {
            Player p = Bukkit.getPlayer(target);
            if (p == null) {
                event.reply("❌ Player **" + target + "** is not online.").setEphemeral(true).queue();
                return;
            }

            boolean unfrozen = plugin.getFreezeManager().unfreezePlayer(p);
            if (unfrozen) {
                event.reply("✅ Successfully unfrozen **" + p.getName() + "**.").queue();
            } else {
                event.reply("⚠️ Player **" + p.getName() + "** was not frozen.").setEphemeral(true).queue();
            }
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (!buttonId.startsWith("argus:")) {
            return;
        }

        Member member = event.getMember();
        if (member == null || (!member.hasPermission(Permission.ADMINISTRATOR) && !hasStaffRole(member))) {
            event.reply("⛔ **Unauthorized:** Only authorized staff may trigger remote anti-cheat actions.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String[] parts = buttonId.split(":");
        if (parts.length < 2) return;

        String action = parts[1].toLowerCase();
        String targetPlayer = parts.length > 2 ? parts[2] : "";

        switch (action) {
            case "ban" -> {
                plugin.getScheduler().runGlobal(() -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + targetPlayer + " Cheating | ArgusAC Remote Discord Action");
                });
                event.reply("🔴 Remote enforcement executed: **" + targetPlayer + "** has been banned by " + event.getUser().getAsMention()).queue();
            }
            case "kick" -> {
                plugin.getScheduler().runGlobal(() -> {
                    Player p = Bukkit.getPlayer(targetPlayer);
                    if (p != null) {
                        p.kickPlayer(ChatColor.RED + "[ArgusAC] Disconnected by Discord Staff Action.");
                    }
                });
                event.reply("🟡 Remote enforcement executed: **" + targetPlayer + "** has been kicked.").queue();
            }
            case "freeze" -> {
                plugin.getScheduler().runGlobal(() -> {
                    Player p = Bukkit.getPlayer(targetPlayer);
                    if (p != null) {
                        String code = plugin.getFreezeManager().freezePlayer(p, event.getUser().getAsTag() + " (Discord)");
                        event.reply("❄️ **" + targetPlayer + "** has been frozen in-game. Verification code: `" + code + "`").queue();
                    } else {
                        event.reply("❌ Player **" + targetPlayer + "** is no longer online.").setEphemeral(true).queue();
                    }
                });
            }
            case "lookup" -> {
                event.deferReply(true).queue();
                plugin.getScheduler().runAsync(() -> {
                    List<PlayerRecord> records = plugin.getAltManager().getRecordsByUsername(targetPlayer);
                    if (records.isEmpty()) {
                        event.getHook().sendMessage("🔍 No historical records found for **" + targetPlayer + "**.").queue();
                        return;
                    }
                    StringBuilder sb = new StringBuilder("📋 **Linked Account Audit for " + targetPlayer + ":**\n");
                    for (PlayerRecord rec : records) {
                        sb.append("• `").append(rec.username()).append("` (").append(rec.uuid()).append(")\n");
                    }
                    event.getHook().sendMessage(sb.toString()).queue();
                });
            }
            case "dismiss" -> {
                event.getMessage().delete().queue(
                        v -> event.reply("🗑️ Alert dismissed.").setEphemeral(true).queue(),
                        err -> event.reply("Failed to dismiss alert: " + err.getMessage()).setEphemeral(true).queue()
                );
            }
        }
    }

    private boolean hasStaffRole(Member member) {
        String staffRoleId = plugin.getConfigManager().getDiscord().getString("bot.roles.staff-admin", "");
        if (staffRoleId.isBlank() || staffRoleId.equals("000000000000000000")) {
            return false;
        }
        return member.getRoles().stream().anyMatch(role -> role.getId().equals(staffRoleId));
    }
}
