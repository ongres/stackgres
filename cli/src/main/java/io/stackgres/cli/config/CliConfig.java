package io.stackgres.cli.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The CLI's on-disk config: {@code current-context} plus a list of named {@link Context}s, kubectl/
 * gcloud-style. Stored at {@code ~/.stackgres/config.yaml} (override with {@code STACKGRES_CONFIG},
 * handy for tests). Parsed and written as plain maps/lists via the snakeyaml already on the classpath
 * — no bean binding — so it stays native-image safe. The {@code context} commands mutate and
 * {@link #save()} it; {@code io.stackgres.cli.CliContext} reads it (never writes) during resolution.
 */
public final class CliConfig {

    /** The context name used when none is current — materialized on demand so no setup is needed. */
    public static final String DEFAULT_CONTEXT = "default";

    private String currentContext;
    private final List<Context> contexts;

    public CliConfig(String currentContext, List<Context> contexts) {
        this.currentContext = currentContext;
        this.contexts = new ArrayList<>(contexts);
    }

    public static Path configPath() {
        String override = System.getenv("STACKGRES_CONFIG");
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        return Paths.get(System.getProperty("user.home"), ".stackgres", "config.yaml");
    }

    @SuppressWarnings("unchecked")
    public static CliConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return new CliConfig(null, new ArrayList<>());
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            Object data = new Yaml().load(reader);
            if (!(data instanceof Map)) {
                return new CliConfig(null, new ArrayList<>());
            }
            Map<String, Object> root = (Map<String, Object>) data;
            String current = str(root, "current-context");
            List<Context> parsed = new ArrayList<>();
            Object rawList = root.get("contexts");
            if (rawList instanceof List<?> list) {
                for (Object element : list) {
                    if (element instanceof Map<?, ?> m) {
                        parsed.add(new Context(str(m, "name"), str(m, "endpoint"),
                                boolOrNull(m, "tls"), str(m, "token"), str(m, "environment")));
                    }
                }
            }
            return new CliConfig(current, parsed);
        } catch (IOException e) {
            throw new RuntimeException("failed to read config " + path + ": " + e.getMessage(), e);
        }
    }

    public void save() {
        Path path = configPath();
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Map<String, Object> root = new LinkedHashMap<>();
            if (currentContext != null) {
                root.put("current-context", currentContext);
            }
            List<Map<String, Object>> list = new ArrayList<>();
            for (Context c : contexts) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", c.name());
                putIfPresent(m, "endpoint", c.endpoint());
                putIfPresent(m, "tls", c.tls());
                putIfPresent(m, "token", c.token());
                putIfPresent(m, "environment", c.environment());
                list.add(m);
            }
            root.put("contexts", list);
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            try (Writer writer = Files.newBufferedWriter(path)) {
                new Yaml(options).dump(root, writer);
            }
            restrictPermissions(path);   // the file may hold a token
        } catch (java.nio.file.AccessDeniedException e) {
            // The parent dir (or file) exists but isn't writable by this user — commonly a
            // ~/.stackgres left behind, owned by root, from a process that ran elevated.
            Path parent = path.getParent();
            throw new RuntimeException("cannot write config " + path
                    + " — permission denied. Check ownership: `ls -ld " + parent + "`; if it is not"
                    + " owned by you, fix it with `sudo chown -R $USER " + parent + "` (or remove it: `rm -rf "
                    + parent + "`).", e);
        } catch (IOException e) {
            throw new RuntimeException("failed to write config " + path + ": "
                    + e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : ""), e);
        }
    }

    public String currentContext() {
        return currentContext;
    }

    /** The context to persist state into: the current one if set, else {@link #DEFAULT_CONTEXT}. */
    public String writeContextName() {
        return currentContext != null && !currentContext.isBlank() ? currentContext : DEFAULT_CONTEXT;
    }

    public List<Context> contexts() {
        return List.copyOf(contexts);
    }

    public Optional<Context> find(String name) {
        return contexts.stream().filter(c -> c.name().equals(name)).findFirst();
    }

    /** Set the active context; the name must already exist. */
    public void use(String name) {
        if (find(name).isEmpty()) {
            throw new IllegalArgumentException("no such context: " + name);
        }
        this.currentContext = name;
    }

    /** Add or replace a context by name. */
    public void upsert(Context context) {
        contexts.removeIf(c -> c.name().equals(context.name()));
        contexts.add(context);
    }

    /** Remove a context; clears current-context if it was the active one. Returns true if removed. */
    public boolean remove(String name) {
        boolean removed = contexts.removeIf(c -> c.name().equals(name));
        if (removed && name.equals(currentContext)) {
            currentContext = null;
        }
        return removed;
    }

    private static void putIfPresent(Map<String, Object> m, String key, Object value) {
        if (value != null) {
            m.put(key, value);
        }
    }

    private static String str(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    private static Boolean boolOrNull(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            return null;
        }
        return v instanceof Boolean b ? b : Boolean.valueOf(v.toString());
    }

    private static void restrictPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignore) {
            // non-POSIX filesystem (e.g. Windows) — best effort only
        }
    }
}
