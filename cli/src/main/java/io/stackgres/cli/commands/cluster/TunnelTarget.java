package io.stackgres.cli.commands.cluster;

import java.util.stream.Stream;

public enum TunnelTarget {

    POSTGRES("postgres"),
    IVORY_SQL("ivorysql");

    private final String id;

    TunnelTarget(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static TunnelTarget fromId(String id) {
        if (id == null || id.isBlank())
            return null;
        return Stream.of(values())
                .filter(t -> t.id.equals(id))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unknown tunnel target: " + id));
    }
}
