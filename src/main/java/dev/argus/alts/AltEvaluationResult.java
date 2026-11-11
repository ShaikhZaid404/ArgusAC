package dev.argus.alts;

/**
 * Sealed result hierarchy for connection evaluations.
 */
public sealed interface AltEvaluationResult permits AltEvaluationResult.Allowed, AltEvaluationResult.Rejected {

    record Allowed(boolean isPrimary, String primaryAccount, int totalAccountsOnIp) implements AltEvaluationResult {}

    record Rejected(String action, String primaryAccount, String disconnectMessage) implements AltEvaluationResult {}
}
