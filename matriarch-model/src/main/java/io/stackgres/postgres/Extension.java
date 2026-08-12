package io.stackgres.postgres;

import io.stackgres.Strings;

import java.util.Arrays;
import java.util.List;

public record Extension(String name, String version, String revision) {

    public Extension {
        if (Strings.isBlank(name))
            throw new IllegalArgumentException("Extension name cannot be blank");
        if (version != null && version.isBlank())
            throw new IllegalArgumentException("Extension version cannot be blank");
        if (revision != null && revision.isBlank())
            throw new IllegalArgumentException("Extension revision cannot be blank");
    }

    @Override
    public String toString() {
        if (version == null && revision == null)
            return name;
        if (revision == null)
            return name + '@' + version;
        return name + '@' + version + '@' + revision;
    }

    private static String extractName(String extension) {
        return extension.split("@")[0];
    }

    private static String extractVersion(String extension) {
        String[] parts = extension.split("@");
        if (parts.length < 2) return null;
        return parts[1];
    }

    private static String extractRevision(String extension) {
        String[] parts = extension.split("@");
        if (parts.length < 3) return null;
        return parts[2];
    }

    public static Extension of(String extension) {
        return new Extension(extractName(extension), extractVersion(extension), extractRevision(extension));
    }

    public static List<Extension> extractExtensions(List<String> extensions) {
        return extensions.stream()
                .sorted()
                .map(Extension::of)
                .toList();
    }

    public static List<Extension> extractExtensions(String commaSeparatedList) {
        return Arrays.stream(commaSeparatedList.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .sorted()
                .map(Extension::of)
                .toList();
    }

}