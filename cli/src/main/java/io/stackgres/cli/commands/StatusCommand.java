package io.stackgres.cli.commands;

import io.stackgres.cli.CliContext;
import io.stackgres.cli.Jwt;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.config.ResolvedContext;
import io.stackgres.cli.postgres.EnvironmentInfo;
import picocli.CommandLine.Command;

import java.util.List;

/**
 * The state of affairs at a glance: where the CLI points (endpoint), who you are (read from the bearer
 * JWT, no server call), which named context is active, and — only when one is pinned — which
 * environment commands target (the header {@code Environment} line; omitted when unset, i.e. all),
 * followed by the environments the matriarch/cloud exposes. Reachability is implied by the environment
 * list and only reported when it fails. Verbose diagnostics belong in {@code info}.
 */
@Command(name = "status", description = "Shows the current endpoint, user, and reachable environments")
public class StatusCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Override
    public void run() {
        if (debug) client.setDebug();
        ResolvedContext ctx = CliContext.resolve();

        field("Endpoint", ctx.endpoint() + (ctx.tls() ? "" : " (plaintext)"));
        field("User", user(ctx));
        field("Context", ctx.contextName() == null ? "(none)" : ctx.contextName());
        // Only shown when a specific environment is pinned; omitted (implicitly "all") when unset.
        if (ctx.environment() != null && !ctx.environment().isBlank()) {
            field("Environment", ctx.environment());
        }
        outln("");

        List<EnvironmentInfo> environments;
        try {
            environments = client.listEnvironments();
        } catch (RuntimeException e) {
            outln("Unreachable: " + e.getMessage());
            return;
        }

        if (environments.isEmpty()) {
            outln("No environments.");
            return;
        }
        outln("Environments:\n");
        EnvironmentTable.print(environments, this::outln);
        // Flag the active environment if it's stale (disconnected / gone) and point at a connected one.
        client.warnIfEnvironmentStale(ctx.environment(), environments);
    }

    private static String user(ResolvedContext ctx) {
        if (ctx.token() == null) {
            return "anonymous";
        }
        String subject = Jwt.subject(ctx.token());
        return subject != null ? subject : "(token set, not a JWT)";
    }
}
