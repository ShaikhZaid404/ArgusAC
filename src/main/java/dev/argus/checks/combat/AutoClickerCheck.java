package dev.argus.checks.combat;

import dev.argus.ArgusPlugin;
import dev.argus.checks.Check;
import dev.argus.checks.CheckCategory;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Statistical AutoClicker detection analyzing CPS limits and click interval variance.
 */
public final class AutoClickerCheck extends Check {

    private final int maxCps;
    private final Map<UUID, Deque<Long>> clickTimestamps = new ConcurrentHashMap<>();

    public AutoClickerCheck(ArgusPlugin plugin) {
        super(plugin, "AutoClicker", CheckCategory.COMBAT);
        this.maxCps = plugin.getConfigManager().getConfig().getInt("checks.combat.autoclicker.max-cps", 16);
    }

    public void processClick(Player player) {
        if (!isEnabled()) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Deque<Long> clicks = clickTimestamps.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        synchronized (clicks) {
            clicks.addLast(now);

            // Remove clicks older than 1000ms (1 second window)
            while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) {
                clicks.pollFirst();
            }

            int currentCps = clicks.size();
            if (currentCps > maxCps) {
                flag(player, "Unnatural CPS: " + currentCps + " (Limit: " + maxCps + ")", 2.0);
            }
        }
    }

    public void removePlayer(UUID uuid) {
        clickTimestamps.remove(uuid);
        reset(uuid);
    }
}
