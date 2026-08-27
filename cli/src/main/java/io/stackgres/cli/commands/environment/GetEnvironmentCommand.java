package io.stackgres.cli.commands.environment;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.EnvironmentInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Command(name = "get", description = "Displays a single environment")
public class GetEnvironmentCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(index = "0", paramLabel = "<id>", description = "The environment id")
    String id;

    @Override
    public void run() {
        if (debug) client.setDebug();
        EnvironmentInfo e = client.getEnvironment(id);
        outf("%-11s%s\n", "ID", e.id());
        outf("%-11s%s\n", "Kind", e.kind());
        outf("%-11s%s\n", "Source", e.source());
        outf("%-11s%s\n", "Health", e.health());
        outf("%-11s%s\n", "As Of", e.asOf() == null ? "" : formatInstant(e.asOf()));
        outf("%-11s%s\n", "Surfaces", String.join(", ", e.surfaces()));
    }

    // "2026-08-25T05:07:35.298Z" -> "2026-08-25 05:07:35 UTC"
    private static String formatInstant(Instant t) {
        return t.truncatedTo(ChronoUnit.SECONDS).toString().replace('T', ' ').replace("Z", " UTC");
    }
}
