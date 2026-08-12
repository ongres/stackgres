package io.stackgres.cli.postgres;

public enum SlonyStatus {

    ACTIVE("Active"),
    INACTIVE("Inactive"),
    DISCONNECTED("Disconnected"),
    UNKNOWN("Unknown");

    private final String displayName;

    SlonyStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

}