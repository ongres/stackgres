package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.postgres.Flavor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "Lists the available PostgreSQL versions")
public class ListVersionsCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Option(names = {"-F", "--flavor"}, description = "The database flavor (postgres|ivorysql; default: postgres)")
    String flavor;

    @Override
    public void run() {
        if (debug) client.setDebug();
        client.listAvailableVersions(Flavor.fromId(flavor)).forEach(this::outln);
    }

}