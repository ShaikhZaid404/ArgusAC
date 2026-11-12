package dev.argus.listeners;

import dev.argus.ArgusPlugin;
import dev.argus.alts.AltEvaluationResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Handles connection authentication, alt-account policy enforcement, and operator welcome banners.
 */
public final class PlayerConnectionListener implements Listener {

    private final ArgusPlugin plugin;

    public PlayerConnectionListener(ArgusPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        InetAddress address = event.getAddress();
        if (address == null) return;

        String ip = address.getHostAddress();

        // Evaluate connection against SQLite Alt Ledger
        AltEvaluationResult result = plugin.getAltManager().evaluateConnection(uuid, name, ip);

        if (result instanceof AltEvaluationResult.Rejected rejected) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, rejected.disconnectMessage());
            if (plugin.getDiscordManager() != null) {
                plugin.getDiscordManager().sendAltAudit(rejected.primaryName(), name, "BLOCKED_ALT_CONNECTION");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Operator live diagnostics banner
        if (player.hasPermission("argus.admin")) {
            boolean hasAI = plugin.getConfigManager().hasAnyAIKey();

            if (hasAI) {
                player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
                player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "ArgusAC " + ChatColor.GRAY + "v" + plugin.getDescription().getVersion() + " » " + ChatColor.GREEN + "Active & Shielding Server");
                player.sendMessage(ChatColor.GRAY + "Engine: " + ChatColor.WHITE + (plugin.isFolia() ? "Folia (Regionised)" : "Paper 1.21"));
                player.sendMessage(ChatColor.GRAY + "Active AI: " + ChatColor.YELLOW + plugin.getAiEngine().getActiveProviderName() + " (" + plugin.getAiEngine().getActiveModelName() + ")");
                player.sendMessage(ChatColor.GRAY + "Prompt Profile: " + ChatColor.AQUA + plugin.getPromptManager().getActiveTier());
                player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
            } else {
                player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
                player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "ArgusAC " + ChatColor.GRAY + "v" + plugin.getDescription().getVersion() + " » " + ChatColor.YELLOW + "Configuration Notice");
                player.sendMessage(ChatColor.RED + "⚠ No AI API keys detected in secrets.yml. Pure Kinematic Heuristics engaged.");

                Component guideLink = Component.text()
                        .append(Component.text("Run ", NamedTextColor.GRAY))
                        .append(Component.text("/argus guide ", NamedTextColor.AQUA))
                        .append(Component.text("[CLICK HERE FOR SETUP BOOK]", NamedTextColor.YELLOW, TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.runCommand("/argus guide")))
                        .build();

                player.sendMessage(guideLink);
                player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getFreezeManager().handleQuit(player);

        UUID uuid = player.getUniqueId();
        plugin.getCheckManager().getKillAuraCheck().removePlayer(uuid);
        plugin.getCheckManager().getAutoClickerCheck().removePlayer(uuid);
        plugin.getCheckManager().getFlyCheck().removePlayer(uuid);
    }
}
