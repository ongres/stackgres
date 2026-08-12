package io.stackgres.postgres;

import java.util.List;

public class PostgresVersions {

    private static final List<String> versions = List.of(
            "15.7", "15.8",
            "16.2", "16.3"
    );

    public static List<String> listAvailableVersions() {
        return versions;
    }

    public boolean isValidVersion(String version) {
        return listAvailableVersions().stream().anyMatch(v -> v.equals(version));
    }

}