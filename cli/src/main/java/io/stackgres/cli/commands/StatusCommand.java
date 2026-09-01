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
 * JWT, no server call), which named context is active, and the environments the matriarch/cloud exposes.
 * The active environment — the one writes target — is marked with {@code *}; reachability is implied by
 * the environment list and only reported when it fails. Verbose diagnostics belong in {@code info}.
 */
@Command(name = "status", description = "Shows the current endpoint, user, and reachable environments")
public class StatusCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Override
    public void run() {
        if (debug) client.setDebug();
        ResolvedContext ctx = CliContext.resolve();

        outf("%-11s%s\n", "Endpoint", ctx.endpoint() + (ctx.tls() ? "" : " (plaintext)"));
        outf("%-11s%s\n", "User", user(ctx));
        outf("%-11s%s\n", "Context", ctx.contextName() == null ? "(none)" : ctx.contextName());
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
