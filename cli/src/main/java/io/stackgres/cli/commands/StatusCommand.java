package io.stackgres.cli.commands;

import io.stackgres.cli.CliContext;
import io.stackgres.cli.Jwt;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.config.CliConfig;
import io.stackgres.cli.config.ResolvedContext;
import io.stackgres.cli.postgres.EnvironmentInfo;
import picocli.CommandLine.Command;

import java.util.List;

/**
 * The "state of affairs" at a glance (tsh/gh/oc style): which context is active, where it points, who
 * you are (read from the bearer JWT, no server call), which environment is targeted and how that
 * resolved, whether the matriarch/cloud is reachable, and the environments it exposes. Provenance
 * ({@code [flag]}/{@code [env]}/{@code [context:name]}/{@code [default]}) comes from
 * {@link CliContext#resolve()}. Verbose diagnostics belong in {@code info}.
 */
@Command(name = "status", description = "Shows the current context, target, user, and reachable environments")
public class StatusCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Override
    public void run() {
        if (debug) client.setDebug();
        ResolvedContext ctx = CliContext.resolve();

        outf("%-14s%s\n", "Context",
                ctx.contextName() == null ? "(none)" : ctx.contextName() + "  (" + CliConfig.configPath() + ")");
        outf("%-14s%s\n", "Endpoint", ctx.endpoint() + (ctx.tls() ? "  (TLS)" : "  (plaintext)") + source(ctx, "endpoint"));
        outf("%-14s%s\n", "User", user(ctx));
        String env = ctx.environment() == null || ctx.environment().isBlank() ? "(all environments)" : ctx.environment();
        outf("%-14s%s\n", "Environment", env + source(ctx, "environment"));

        long start = System.nanoTime();
        List<EnvironmentInfo> environments;
        try {
            environments = client.listEnvironments();
        } catch (RuntimeException e) {
            outf("%-14s%s\n", "Matriarch", "unreachable: " + e.getMessage());
            return;
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        outf("%-14s%s\n", "Matriarch", "reachable (" + ms + " ms)");

        outln("");
        if (environments.isEmpty()) {
            outln("No environments visible.");
            return;
        }
        outf("Environments (%d):\n", environments.size());
        int idLen = width(environments, EnvironmentInfo::id, "ID", 12);
        int kindLen = width(environments, EnvironmentInfo::kind, "KIND", 14);
        String fmt = "%-3s%-" + idLen + "s%-" + kindLen + "s%-8s\n";
        outf(fmt, "", "ID", "KIND", "SOURCE");
        String target = ctx.environment();
        for (EnvironmentInfo e : environments) {
            outf(fmt, e.id().equals(target) ? "*" : "", e.id(), e.kind(), e.source());
        }
    }

    private static String user(ResolvedContext ctx) {
        if (ctx.token() == null) {
            return "anonymous (no token)";
        }
        String subject = Jwt.subject(ctx.token());
        return subject != null ? subject + "  (from token)" : "(token set, not a JWT)";
    }

    private static String source(ResolvedContext ctx, String field) {
        String s = ctx.sources().get(field);
        return s == null ? "" : "  [" + s + "]";
    }

    private static int width(List<EnvironmentInfo> rows, java.util.function.Function<EnvironmentInfo, String> field, String header, int min) {
        int max = rows.stream().map(field).filter(java.util.Objects::nonNull).mapToInt(String::length).max().orElse(0);
        return Math.max(Math.max(max, header.length()), min) + 2;
    }
}
