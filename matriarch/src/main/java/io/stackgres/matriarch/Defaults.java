package io.stackgres.matriarch;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Cluster-creation defaults the matriarch fills for whatever the client leaves blank (§5.1),
 * centralized here rather than scattered as literals in the resolver — mirroring the existing
 * matriarch's {@code clusters/Defaults}. (Engine version and extensions are resolved via their
 * catalogs, not fixed here.) A plain constants holder for now; it can become a config-driven
 * component if per-deployment overrides are wanted.
 */
public final class Defaults {

    private Defaults() {
    }

    public static final String LISTEN_ADDRESS = "0.0.0.0";

    public static final String USERNAME = "postgres";

    /** Mint a superuser password when the client doesn't supply one — mirrors the old matriarch. */
    public static String generatePassword() {
        return UUID.randomUUID().toString().substring(4, 22);
    }

    private static final String NAME_PREFIX = "default";
    private static final Pattern NAME_PATTERN = Pattern.compile("^" + NAME_PREFIX + "\\d*$");

    /** The next available {@code default}, {@code default1}, {@code default2}… among {@code existingNames}. */
    public static String nextName(Set<String> existingNames) {
        Set<String> defaults = existingNames.stream()
                .filter(n -> n != null && NAME_PATTERN.matcher(n).matches())
                .collect(Collectors.toSet());
        int maxIndex = defaults.stream()
                .map(n -> n.replaceAll("\\D", ""))
                .filter(digits -> !digits.isBlank())
                .mapToInt(Integer::parseInt)
                .max().orElse(-1);
        if (maxIndex < 0) {
            return defaults.contains(NAME_PREFIX) ? NAME_PREFIX + 1 : NAME_PREFIX;
        }
        return NAME_PREFIX + (maxIndex + 1);
    }
}
