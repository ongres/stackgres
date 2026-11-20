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

    @Parameters(index = "0", paramLabel = "<name>", description = "The environment name")
    String name;

    @Override
    public void run() {
        if (debug) client.setDebug();
        EnvironmentInfo e = client.getEnvironment(name);
        field("Name", e.name());
        field("Kind", e.kind());
        field("Status", e.status());
        field("Last seen", Times.stampAndAgo(e.asOf()));
        field("Surfaces", String.join(", ", e.surfaces()));
        if (!e.tags().isEmpty()) {
            field("Tags", e.tags().entrySet().stream()
                    .map(t -> t.getKey() + "=" + t.getValue())
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(", ")));
        }
    }
}
