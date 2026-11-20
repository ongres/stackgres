package io.stackgres.systemtest;

import java.time.Duration;

/**
 * The single config seam: every knob is a system property (e.g. {@code -Dstackgres.systemtest.target=cloud})
 * with an env-var fallback ({@code STACKGRES_SYSTEMTEST_TARGET}) and a default. Nothing else in the suite
 * reads the environment — tests are written against the resolved {@link Target}, so flipping direct↔cloud
 * is purely config.
 */
public final class TestConfig {

    private TestConfig() {
    }

    /** direct | cloud. */
    public static final Target TARGET = Target.valueOf(prop("stackgres.systemtest.target", "direct").toUpperCase());

    /** mock | real — whether the suite starts an in-process mock agent or expects a real slony running. */
    public static final AgentMode AGENT = AgentMode.valueOf(prop("stackgres.systemtest.agent", "mock").toUpperCase());

    /** {@code host:port} the CLI and gRPC client connect to (matriarch :9000 direct; cloud host :443). */
    public static final String ENDPOINT = prop("stackgres.systemtest.endpoint", "localhost:9000");

    /** TLS on the endpoint (false direct / plaintext; true for the cloud). */
    public static final boolean TLS = Boolean.parseBoolean(prop("stackgres.systemtest.tls", "false"));

    /** Pre-issued JWT for the cloud target (empty = none, as for a direct matriarch). */
    public static final String TOKEN = prop("stackgres.systemtest.token", "");

    /** Name of the environment to scope to (cloud, when several exist); empty = default/implicit. */
    public static final String ENVIRONMENT = prop("stackgres.systemtest.environment", "");

    /** Base URL for the cloud's REST endpoints (e.g. {@code GET /events}); derived from the endpoint host. */
    public static final String HTTP_BASE =
            prop("stackgres.systemtest.http-base", (TLS ? "https://" : "http://") + host());

    /** The {@code stackgres} CLI binary (a path or a name on $PATH; a wrapper script is fine). */
    public static final String CLI = prop("stackgres.systemtest.cli", "stackgres");

    public static final Duration COMMAND_TIMEOUT =
            Duration.ofSeconds(Long.parseLong(prop("stackgres.systemtest.command-timeout-seconds", "30")));
    public static final Duration HEALTHY_TIMEOUT =
            Duration.ofSeconds(Long.parseLong(prop("stackgres.systemtest.healthy-timeout-seconds", "60")));

    public static String host() {
        int i = ENDPOINT.lastIndexOf(':');
        return i > 0 ? ENDPOINT.substring(0, i) : ENDPOINT;
    }

    public static int port() {
        int i = ENDPOINT.lastIndexOf(':');
        if (i > 0) {
            return Integer.parseInt(ENDPOINT.substring(i + 1));
        }
        return TLS ? 443 : 9000;   // no port given: assume the TLS/plaintext default
    }

    private static String prop(String key, String def) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) {
            v = System.getenv(key.toUpperCase().replace('.', '_').replace('-', '_'));
        }
        return v == null || v.isBlank() ? def : v;
    }
}
