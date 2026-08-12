package io.stackgres.cli.client;

public record ClusterCreationUpdate(String id, String name, String password, Status status, Object[] details) {

    public ClusterCreationUpdate(String id, String name, String password, Status status) {
        this(id, name, password, status, new Object[]{});
    }

    public enum Status {
        CREATED, PENDING, HEALTHY;

        public static Status ofGrpcStatusType(String type) {
            return switch (type) {
                case "CREATED" -> CREATED;
                case "PENDING" -> PENDING;
                case "HEALTHY" -> HEALTHY;
                default -> throw new IllegalArgumentException("Unknown status type: " + type);
            };
        }
    }

}