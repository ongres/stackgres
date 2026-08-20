package io.stackgres.cli;

import io.stackgres.cli.config.CliConfig;
import io.stackgres.cli.config.Context;
import io.stackgres.cli.config.ResolvedContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Process-wide resolution of the CLI's connection target. Global options ({@code --context},
 * {@code --endpoint}, {@code --token}, {@code -E/--environment}) are captured here by their setter and,
 * because the CLI runs one command per process, kept in statics. The target is resolved lazily on first
 * use — after picocli has injected the options — by {@link #resolve()}, which applies precedence
 * <em>flag &gt; env var &gt; current context ({@code ~/.stackgres/config.yaml}) &gt; default</em> per
 * field and records provenance for {@code status}/{@code info}. The result is cached (one invocation =
 * one target).
 */
public final class CliContext {

    private static volatile String contextOverride;
    private static volatile String endpointOverride;
    private static volatile String tokenOverride;
    private static volatile String environmentOverride;

    private static volatile ResolvedContext resolved;

    private CliContext() {
    }

    // --- setters, called by the global @Option method-setters on StackGresBaseCommand ---

    public static void setContext(String context) {
        contextOverride = blankToNull(context);
    }

    public static void setEndpoint(String endpoint) {
        endpointOverride = blankToNull(endpoint);
    }

    public static void setToken(String token) {
        tokenOverride = blankToNull(token);
    }

    public static void setEnvironment(String environment) {
        environmentOverride = blankToNull(environment);
    }

    /** The resolved environment id used for api.v1 requests (convenience for {@link #resolve()}). */
    public static String environment() {
        return resolve().environment();
    }

    /** Resolve (and cache) the effective target for this invocation. */
    public static ResolvedContext resolve() {
        ResolvedContext r = resolved;
        if (r == null) {
            r = compute();
            resolved = r;
        }
        return r;
    }

    private static ResolvedContext compute() {
        CliConfig config = CliConfig.load();
        Map<String, String> sources = new LinkedHashMap<>();

        String contextName = first(contextOverride, System.getenv("STACKGRES_CONTEXT"), config.currentContext());
        Context ctx = contextName == null ? null : config.find(contextName).orElse(null);
        String ctxSource = "context:" + contextName;

        String endpoint = resolveString(sources, "endpoint",
                endpointOverride, System.getenv("STACKGRES_ENDPOINT_URL"),
                ctx == null ? null : ctx.endpoint(), ctxSource, "localhost:50051");

        String token = resolveString(sources, "token",
                tokenOverride, System.getenv("STACKGRES_TOKEN"),
                ctx == null ? null : ctx.token(), ctxSource, null);

        // Default is UNSET (""), meaning "all environments" for list and "resolve or require one" for
        // single-cluster / mutating ops (kubectl-namespace model). Never silently "local".
        String environment = resolveString(sources, "environment",
                environmentOverride, System.getenv("STACKGRES_ENVIRONMENT"),
                ctx == null ? null : ctx.environment(), ctxSource, "");

        boolean tls = resolveTls(sources, ctx, endpoint, ctxSource);

        if (contextName != null) {
            sources.put("context", config.find(contextName).isPresent() ? "config" : "unknown");
        }
        return new ResolvedContext(contextName, endpoint, tls, token, environment, sources);
    }

    private static String resolveString(Map<String, String> sources, String field, String flag, String env,
                                        String ctx, String ctxSource, String def) {
        if (flag != null) {
            sources.put(field, "flag");
            return flag;
        }
        if (env != null && !env.isBlank()) {
            sources.put(field, "env");
            return env;
        }
        if (ctx != null && !ctx.isBlank()) {
            sources.put(field, ctxSource);
            return ctx;
        }
        sources.put(field, "default");
        return def;
    }

    private static boolean resolveTls(Map<String, String> sources, Context ctx, String endpoint, String ctxSource) {
        String env = System.getenv("STACKGRES_ENDPOINT_TLS");
        if (env != null && !env.isBlank()) {
            sources.put("tls", "env");
            return Boolean.parseBoolean(env);
        }
        if (ctx != null && ctx.tls() != null) {
            sources.put("tls", ctxSource);
            return ctx.tls();
        }
        sources.put("tls", "auto");   // TLS unless plainly localhost
        return !endpoint.startsWith("localhost:");
    }

    private static String first(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /** Test hook: drop the cached resolution so a following resolve() re-reads overrides/config. */
    public static Optional<ResolvedContext> cached() {
        return Optional.ofNullable(resolved);
    }
}
