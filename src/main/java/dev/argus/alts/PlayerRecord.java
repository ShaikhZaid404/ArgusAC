package dev.argus.alts;

import java.util.UUID;

/**
 * Immutable player ledger identity record stored in database.
 */
public record PlayerRecord(
        UUID uuid,
        String username,
        String ipAddress,
        long firstSeen,
        long lastSeen,
        int trustScore,
        int flagsCount
) {
    public boolean isPrimary(PlayerRecord other) {
        return this.firstSeen <= other.firstSeen;
    }
}
