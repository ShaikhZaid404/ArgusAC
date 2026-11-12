package dev.argus.commands;

import dev.argus.ArgusPlugin;
import dev.argus.alts.PlayerRecord;
import dev.argus.ui.SetupBook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Master command executor and tab-completer for /argus.
 */
public final class ArgusCommand implements CommandExecutor, TabCompleter {

    private final ArgusPlugin plugin;

    public ArgusCommand(ArgusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "guide" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "The guide book can only be received by players in-game.");
                    return true;
                }
                player.getInventory().addItem(SetupBook.createGuideBook(plugin));
                player.sendMessage(ChatColor.GREEN + "[ArgusAC] Received the interactive setup guide book!");
            }
            case "reload" -> {
                long start = System.currentTimeMillis();
                plugin.getConfigManager().reloadAll();
                plugin.getPromptManager().loadPrompts();
                plugin.getCheckManager().reloadChecks();
                if (plugin.getDiscordManager() != null) {
                    plugin.getDiscordManager().reload();
                }
                long elapsed = System.currentTimeMillis() - start;

                sender.sendMessage(ChatColor.GREEN + "[ArgusAC] Zero-downtime reload complete in " + elapsed + "ms! (Active Checks: "
                        + plugin.getCheckManager().getChecks().size() + ", AI: " + plugin.getAiEngine().getActiveProviderName() + ")");
            }
            case "freeze" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /argus freeze <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found online.");
                    return true;
                }
                String code = plugin.getFreezeManager().freezePlayer(target, sender.getName());
                sender.sendMessage(ChatColor.GREEN + "[ArgusAC] Frozen " + target.getName() + "! Verification code: " + ChatColor.YELLOW + code);
            }
            case "unfreeze" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /argus unfreeze <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found online.");
                    return true;
                }
                boolean unfreezed = plugin.getFreezeManager().unfreezePlayer(target);
                if (unfreezed) {
                    sender.sendMessage(ChatColor.GREEN + "[ArgusAC] Successfully unfrozen " + target.getName() + ".");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "[ArgusAC] Player was not frozen.");
                }
            }
            case "alts" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /argus alts <player|ip>");
                    return true;
                }
                String query = args[1];
                plugin.getScheduler().runAsync(() -> {
                    List<PlayerRecord> records = query.contains(".")
                            ? plugin.getAltManager().getRecordsByIp(query)
                            : plugin.getAltManager().getRecordsByUsername(query);

                    if (records.isEmpty()) {
                        sender.sendMessage(ChatColor.YELLOW + "[ArgusAC] No recorded accounts found for query: " + query);
                        return;
                    }

                    sender.sendMessage(ChatColor.AQUA + "=== Linked Accounts for (" + query + ") [" + records.size() + "] ===");
                    for (int i = 0; i < records.size(); i++) {
                        PlayerRecord rec = records.get(i);
                        String role = (i == 0) ? ChatColor.GREEN + " [PRIMARY]" : ChatColor.GRAY + " [ALT]";
                        sender.sendMessage(ChatColor.WHITE + " • " + rec.username() + role + ChatColor.DARK_GRAY + " (" + rec.uuid() + ")");
                    }
                });
            }
            case "efficiency" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /argus efficiency <low|medium|high|extra_high>");
                    return true;
                }
                boolean updated = plugin.getPromptManager().setActiveTier(args[1]);
                if (updated) {
                    sender.sendMessage(ChatColor.GREEN + "[ArgusAC] AI Efficiency hot-swapped to: " + ChatColor.YELLOW + plugin.getPromptManager().getActiveTier());
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid tier. Available options: low, medium, high, extra_high");
                }
            }
            case "status" -> {
                sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "ArgusAC System Status");
                sender.sendMessage(ChatColor.GRAY + "Server Engine: " + ChatColor.WHITE + (plugin.isFolia() ? "Folia (Regionised)" : "Paper 1.21"));
                sender.sendMessage(ChatColor.GRAY + "Active AI: " + ChatColor.YELLOW + plugin.getAiEngine().getActiveProviderName() + " (" + plugin.getAiEngine().getActiveModelName() + ")");
                sender.sendMessage(ChatColor.GRAY + "AI Latency: " + ChatColor.GREEN + plugin.getAiEngine().getLastPingMs() + "ms");
                sender.sendMessage(ChatColor.GRAY + "Efficiency Profile: " + ChatColor.AQUA + plugin.getPromptManager().getActiveTier());
                sender.sendMessage(ChatColor.GRAY + "Loaded Checks: " + ChatColor.WHITE + plugin.getCheckManager().getChecks().size() + " active");
                sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "=== ArgusAC Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/argus guide " + ChatColor.GRAY + "- Receive interactive setup book");
        sender.sendMessage(ChatColor.YELLOW + "/argus status " + ChatColor.GRAY + "- Live diagnostics & AI latency");
        sender.sendMessage(ChatColor.YELLOW + "/argus reload " + ChatColor.GRAY + "- Zero-downtime hot refresh");
        sender.sendMessage(ChatColor.YELLOW + "/argus freeze <player> " + ChatColor.GRAY + "- Lock suspect for screenshare");
        sender.sendMessage(ChatColor.YELLOW + "/argus unfreeze <player> " + ChatColor.GRAY + "- Release frozen player");
        sender.sendMessage(ChatColor.YELLOW + "/argus alts <player|ip> " + ChatColor.GRAY + "- View linked IP accounts");
        sender.sendMessage(ChatColor.YELLOW + "/argus efficiency <tier> " + ChatColor.GRAY + "- Hot-swap prompt tier");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = List.of("guide", "status", "reload", "freeze", "unfreeze", "alts", "efficiency");
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("freeze") || args[0].equalsIgnoreCase("unfreeze")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
            }
            if (args[0].equalsIgnoreCase("efficiency")) {
                return List.of("low", "medium", "high", "extra_high").stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
            }
        }
        return new ArrayList<>();
    }
}
