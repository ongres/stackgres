package io.stackgres.cli.commands.environment;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.EnvironmentTable;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.EnvironmentInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(name = "list", description = "Lists the environments")
public class ListEnvironmentCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Option(names = {"-q", "--quiet"}, description = "Only display the environment IDs")
    boolean quiet;

    @Override
    public void run() {
        if (debug) client.setDebug();
        List<EnvironmentInfo> environments = client.listEnvironments();

        if (quiet) {
            environments.forEach(e -> outln(e.id()));
            return;
        }
        if (environments.isEmpty()) {
            outln("There are no environments");
            return;
        }
        // Same table as `status`; per-field detail (health, as-of, surfaces) is in `environment get`.
        EnvironmentTable.print(environments, this::outln);
    }
}
