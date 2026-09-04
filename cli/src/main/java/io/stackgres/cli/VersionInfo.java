package io.stackgres.cli;

import java.io.InputStream;
import java.util.Properties;

/**
 * The CLI's own build stamp, read once from {@code /stackgres-version.properties} (filtered in by Maven:
 * {@code version}, {@code commit}, {@code built}). Values that weren't stamped — an unresolved
 * {@code ${...}} placeholder, a blank, or the {@code unknown} default (e.g. built without the git
 * commit) — read back as {@code null} so callers can simply omit them. Shared by the {@code version}
 * command and picocli's {@code --version} provider so both agree.
 */
public final class VersionInfo {

    public static final VersionInfo INSTANCE = load();

    private final String version;
    private final String commit;
    private final String built;

    private VersionInfo(String version, String commit, String built) {
        this.version = version;
        this.commit = commit;
        this.built = built;
    }

    /** Build version (e.g. {@code 0.1}); {@code dev} when unstamped. Never null. */
    public String version() {
        return version != null ? version : "dev";
    }

    /** Short git commit, or null when not stamped. */
    public String commit() {
        return commit;
    }

    /** Build timestamp (ISO-8601 UTC), or null when not stamped. */
    public String built() {
        return built;
    }

    /** The JVM/native runtime this binary runs on, e.g. {@code GraalVM native image (Java 25)}. */
    public String runtime() {
        String java = System.getProperty("java.version", "?");
        boolean nativeImage = System.getProperty("org.graalvm.nativeimage.imagecode") != null;
        return nativeImage ? "GraalVM native image (Java " + java + ")" : "JVM (Java " + java + ")";
    }

    /** The OS/arch this binary was built for, e.g. {@code linux/amd64}. */
    public String platform() {
        return System.getProperty("os.name", "?").toLowerCase().split(" ")[0]
                + "/" + System.getProperty("os.arch", "?");
    }

    private static VersionInfo load() {
        Properties p = new Properties();
        try (InputStream in = VersionInfo.class.getResourceAsStream("/stackgres-version.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (Exception ignore) {
            // display-only — a missing/garbled stamp must never break the CLI
        }
        return new VersionInfo(clean(p.getProperty("version")), clean(p.getProperty("commit")),
                clean(p.getProperty("built")));
    }

    /** Null out values that weren't actually stamped: blanks, "unknown", or an unresolved ${...}. */
    private static String clean(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() || t.equals("unknown") || t.startsWith("${") ? null : t;
    }
}
