package io.stackgres.cli.commands;

import io.stackgres.cli.postgres.EnvironmentInfo;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The environment table, shared by {@code status} and {@code environment list} so both render
 * identically. Fuller per-field detail (health, as-of, surfaces) lives in {@code environment get}.
 */
public final class EnvironmentTable {

    private EnvironmentTable() {
    }

    /**
     * Print the count line, a blank line, and the table via {@code out} (e.g. a command's {@code outln}).
     */
    public static void print(List<EnvironmentInfo> environments, Consumer<String> out) {
        int idLen = width(environments, EnvironmentInfo::id, "ID", 8);
        int kindLen = width(environments, EnvironmentInfo::kind, "Kind", 12);
        String fmt = "%-" + idLen + "s%-" + kindLen + "s%-8s";
        out.accept(String.format(fmt, "ID", "Kind", "Source"));
        for (EnvironmentInfo e : environments) {
            out.accept(String.format(fmt, e.id(), e.kind(), e.source()));
        }
    }

    private static int width(List<EnvironmentInfo> rows, Function<EnvironmentInfo, String> field, String header, int min) {
        int max = rows.stream().map(field).filter(Objects::nonNull).mapToInt(String::length).max().orElse(0);
        return Math.max(Math.max(max, header.length()), min) + 2;
    }

}