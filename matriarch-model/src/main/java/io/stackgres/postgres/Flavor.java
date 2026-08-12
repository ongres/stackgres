package io.stackgres.postgres;

import io.stackgres.Strings;

import java.util.stream.Stream;

public enum Flavor {

    POSTGRES("postgres", "PostgreSQL"),
    IVORY_SQL("ivorysql", "IvorySQL");

    private final String id;
    private final String displayName;

    Flavor(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Flavor fromId(String id) {
        if (Strings.isBlank(id))
            return POSTGRES;
        return Stream.of(values())
                .filter(f -> f.id.equals(id))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unknown flavor: " + id));
    }

}