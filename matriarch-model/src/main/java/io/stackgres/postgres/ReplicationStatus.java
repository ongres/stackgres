package io.stackgres.postgres;

public enum ReplicationStatus {
    STANDALONE("Standalone"), PRIMARY("Primary"), REPLICA("Replica"), UNKNOWN("Unknown");

    private final String string;

    ReplicationStatus(String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }
}
