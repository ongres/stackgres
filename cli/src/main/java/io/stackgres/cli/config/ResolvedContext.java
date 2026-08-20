package io.stackgres.cli.config;

import java.util.Map;

/**
 * The effective connection settings for one CLI invocation, after applying precedence
 * (flag &gt; env var &gt; current context &gt; default) across the config file, environment, and global
 * options. {@code sources} records where each value came from (e.g. {@code endpoint -> "context:cloud"})
 * so {@code stackgres status}/{@code info} can show provenance, aws-configure-list style. {@code token}
 * is null when unauthenticated.
 */
public record ResolvedContext(String contextName, String endpoint, boolean tls, String token,
                              String environment, Map<String, String> sources) {
}
