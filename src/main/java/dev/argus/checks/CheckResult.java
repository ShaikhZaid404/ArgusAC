package dev.argus.checks;

import org.bukkit.Location;

/**
 * Sealed hierarchy representing mathematical outcomes of a check validation cycle.
 */
public sealed interface CheckResult permits CheckResult.Legit, CheckResult.Flag, CheckResult.Setback {

    record Legit() implements CheckResult {}

    record Flag(String detail, double vlAdded) implements CheckResult {}

    record Setback(String detail, Location safeLocation) implements CheckResult {}
}
