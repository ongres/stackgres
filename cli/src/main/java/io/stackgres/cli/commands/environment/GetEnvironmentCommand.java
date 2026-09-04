package io.stackgres.cli.commands.environment;

import io.stackgres.cli.Times;
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
        field("ID", e.id());
        field("Kind", e.kind());
        field("Source", e.source());
        field("Health", e.health());
        field("Last seen", Times.stampAndAgo(e.asOf()));
        field("Surfaces", String.join(", ", e.surfaces()));
    }
}
