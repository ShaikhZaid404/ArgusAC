package dev.argus.checks.movement;

import dev.argus.ArgusPlugin;
import dev.argus.checks.Check;
import dev.argus.checks.CheckCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Validates block placement geometry and orientation to catch Scaffold and Tower modules.
 */
public final class ScaffoldCheck extends Check {

    public ScaffoldCheck(ArgusPlugin plugin) {
        super(plugin, "Scaffold", CheckCategory.MOVEMENT);
    }

    public boolean validateBlockPlace(Player player, Block placedBlock) {
        if (!isEnabled()) return true;

        float pitch = player.getLocation().getPitch();
        double playerY = player.getLocation().getY();
        double blockY = placedBlock.getY();

        // Scaffold pattern: placing blocks directly under feet (Y - 1) while sprinting and looking down
        if (blockY < playerY && pitch > 72.0f && player.isSprinting()) {
            flag(player, "Sprint scaffolding (Pitch: " + String.format("%.1f", pitch) + "°)", 2.0);

            // Silent mitigation: cancel illegal block placement if enabled
            boolean cancelBlocks = plugin.getConfigManager().getConfig().getBoolean("mitigation.silent-nerf.cancel-scaffold-blocks", true);
            return !cancelBlocks;
        }

        return true;
    }
}
