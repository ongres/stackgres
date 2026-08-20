package io.stackgres.cli.commands.environment;

import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.config.CliConfig;
import io.stackgres.cli.config.Context;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Sets the active environment for the current context (kubectl {@code set-context --namespace}). With
 * it, single-cluster and mutating commands scope here without {@code -E}, and {@code cluster list} shows
 * just this environment. Omit the id (or pass {@code -}) to clear it — back to "all environments".
 *
 * <p>No ceremony required: if there is no current context yet, a {@code default} one is materialized
 * on the fly. Any connection setting the context doesn't already have is captured from the environment
 * ({@code STACKGRES_ENDPOINT_URL} / {@code STACKGRES_ENDPOINT_TLS}) so the config works in a fresh
 * shell — but the token is never auto-written; it keeps resolving from {@code STACKGRES_TOKEN} / a flag.
 */
@Command(name = "use", description = "Sets (or clears) the active environment for the current context")
public class UseEnvironmentCommand extends StackGresSubCommand {

    @Parameters(index = "0", paramLabel = "<id>", arity = "0..1",
            description = "The environment id; omit or pass '-' to clear (all environments)")
    String id;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        String name = config.writeContextName();   // current context, or "default" (auto-created)
        Context existing = config.find(name).orElse(null);
        String env = (id == null || id.isBlank() || "-".equals(id)) ? null : id;

        // Fill only fields the context doesn't already have, from the environment. Token excluded.
        String endpoint = existing != null && existing.endpoint() != null
                ? existing.endpoint() : envOrNull("STACKGRES_ENDPOINT_URL");
        Boolean tls = existing != null && existing.tls() != null
                ? existing.tls() : boolEnvOrNull("STACKGRES_ENDPOINT_TLS");
        String token = existing == null ? null : existing.token();   // never captured from env

        config.upsert(new Context(name, endpoint, tls, token, env));
        config.use(name);   // materialize as current-context (safe: just upserted)
        config.save();

        if (env == null) {
            outln("Cleared the active environment for context '" + name + "' (now all environments).");
        } else {
            outln("Active environment for context '" + name + "' set to '" + env + "'"
                    + (endpoint != null ? " (endpoint " + endpoint + ")" : "") + ".");
        }
    }

    private static String envOrNull(String key) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? null : v;
    }

    private static Boolean boolEnvOrNull(String key) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? null : Boolean.valueOf(v);
    }
}
