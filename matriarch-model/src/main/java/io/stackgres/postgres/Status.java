package io.stackgres.postgres;

public enum Status {
    CREATED("Created"), PENDING("Pending"), INITDB("InitDB"), STARTED("Started"), HEALTHY("Healthy"), STOPPED("Stopped"), CRASHED("Crashed"), UNKNOWN("Unknown");

    private final String string;

    Status(String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }
}
