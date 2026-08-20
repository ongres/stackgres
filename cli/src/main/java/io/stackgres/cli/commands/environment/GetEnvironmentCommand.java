package io.stackgres.cli.commands.environment;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.EnvironmentInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Displays a single environment")
public class GetEnvironmentCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(index = "0", paramLabel = "<id>", description = "The environment id")
    String id;

    @Override
    public void run() {
        if (debug) client.setDebug();
        EnvironmentInfo e = client.getEnvironment(id);
        outf("%-14s%s\n", "ID", e.id());
        outf("%-14s%s\n", "Kind", e.kind());
        outf("%-14s%s\n", "Source", e.source());
        outf("%-14s%s\n", "Health", e.health());
        outf("%-14s%s\n", "As of", e.asOf() == null ? "" : e.asOf().toString());
        outf("%-14s%s\n", "Surfaces", String.join(", ", e.surfaces()));
    }

}