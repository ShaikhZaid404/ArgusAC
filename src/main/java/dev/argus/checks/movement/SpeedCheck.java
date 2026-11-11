package dev.argus.checks.movement;

import dev.argus.ArgusPlugin;
import dev.argus.checks.Check;
import dev.argus.checks.CheckCategory;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Validates horizontal motion vectors against maximum theoretical kinematics.
 */
public final class SpeedCheck extends Check {

    public SpeedCheck(ArgusPlugin plugin) {
        super(plugin, "Speed", CheckCategory.MOVEMENT);
    }

    public void processMovement(Player player, Location from, Location to) {
        if (!isEnabled()) return;
        if (player.getAllowFlight() || player.getGameMode() == GameMode.CREATIVE || player.isGliding()) return;

        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double horizontalDistance = Math.hypot(deltaX, deltaZ);

        // Vanilla maximum sprint-jump speed baseline is ~0.62 blocks/tick
        double maxAllowed = player.isSprinting() ? 0.65 : 0.38;

        // Potion effect speed multiplier (Speed I = +20%, Speed II = +40%)
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            maxAllowed += amplifier * 0.12;
        }

        // Buffer threshold
        if (horizontalDistance > maxAllowed + 0.15) {
            flag(player, "Excessive speed: " + String.format("%.2f", horizontalDistance) + "b/t (Max: " + String.format("%.2f", maxAllowed) + "b/t)", 1.5);
            plugin.getScheduler().runAtEntity(player, p -> player.teleport(from));
        }
    }
}
