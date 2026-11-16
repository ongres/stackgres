package io.stackgres.systemtest;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version-catalog helpers. DOCIR advertises a {@code latest} alias, per-major {@code <major>.latest} aliases,
 * and concrete {@code <major>.<minor>} versions; {@code latest} resolves to the <em>highest</em> concrete
 * version (numeric compare — major then minor, so {@code 19.11 > 19.2}).
 */
public final class Versions {

    private static final Pattern CONCRETE = Pattern.compile("\\b(\\d+)\\.(\\d+)\\b");

    private Versions() {
    }

    /** The distinct concrete {@code <major>.<minor>} versions appearing in {@code text} (e.g. CLI output). */
    public static List<String> concrete(String text) {
        Matcher m = CONCRETE.matcher(text);
        java.util.LinkedHashSet<String> found = new java.util.LinkedHashSet<>();
        while (m.find()) {
            found.add(m.group(1) + "." + m.group(2));
        }
        return List.copyOf(found);
    }

    /**
     * The highest concrete {@code <major>.<minor>} in {@code versions} (aliases are ignored) — what
     * {@code latest} resolves to — or {@code null} if there is no concrete entry.
     */
    public static String highestConcrete(Collection<String> versions) {
        return versions.stream()
                .filter(v -> v.matches("\\d+\\.\\d+"))
                .max(Comparator.comparingInt(Versions::major).thenComparingInt(Versions::minor))
                .orElse(null);
    }

    private static int major(String v) {
        return Integer.parseInt(v.substring(0, v.indexOf('.')));
    }

    private static int minor(String v) {
        return Integer.parseInt(v.substring(v.indexOf('.') + 1));
    }
}
