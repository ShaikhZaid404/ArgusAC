package dev.argus.checks;

public enum CheckCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    WORLD("World"),
    PACKET("Packet");

    private final String displayName;

    CheckCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
