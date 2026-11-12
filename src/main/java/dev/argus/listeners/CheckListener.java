package dev.argus.listeners;

import dev.argus.ArgusPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Dispatches Bukkit and Paper events into corresponding Argus detection watchers.
 */
public final class CheckListener implements Listener {

    private final ArgusPlugin plugin;

    public CheckListener(ArgusPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Freeze Enforcement
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            Location locked = plugin.getFreezeManager().getFrozenLocation(player.getUniqueId());
            if (locked != null && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
                event.setTo(locked);
                return;
            }
        }

        // Only process if player actually changed coordinates
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            plugin.getCheckManager().getFlyCheck().processMovement(player, from, to);
            plugin.getCheckManager().getSpeedCheck().processMovement(player, from, to);
        }

        // Process rotation delta
        if (from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch()) {
            plugin.getCheckManager().getKillAuraCheck().processRotation(player, to.getYaw(), to.getPitch());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        Entity target = event.getEntity();

        // Reach Check
        boolean reachAllowed = plugin.getCheckManager().getReachCheck().validateAttack(attacker, target);
        if (!reachAllowed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.getCheckManager().getAutoClickerCheck().processClick(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAnimation(org.bukkit.event.player.PlayerAnimationEvent event) {
        if (event.getAnimationType() == org.bukkit.event.player.PlayerAnimationType.ARM_SWING) {
            plugin.getCheckManager().getAutoClickerCheck().processClick(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        boolean blockAllowed = plugin.getCheckManager().getScaffoldCheck().validateBlockPlace(player, event.getBlockPlaced());
        if (!blockAllowed) {
            event.setCancelled(true);
        }
    }
}
