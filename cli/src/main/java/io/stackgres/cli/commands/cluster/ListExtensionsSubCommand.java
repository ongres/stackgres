package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.postgres.Extension;
import io.stackgres.postgres.Flavor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(name = "list", description = "Lists the names and versions of the available extensions")
public class ListExtensionsSubCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Option(names = {"-F", "--flavor"}, description = "The database flavor (postgres|ivorysql; default: postgres)")
    String flavor;

    @Option(names = {"-v", "--version"}, description = "The PostgreSQL version for which to list the extensions (default: latest)", defaultValue = "latest")
    String version;

    @Option(names = {"-V", "--include-versions"}, description = "Include the extension versions")
    boolean includeVersions;

    @Override
    public void run() {
        if (debug) client.setDebug();
        List<Extension> extensions = client.listAvailableExtensions(Flavor.fromId(flavor), version);
        if (!includeVersions) {
            // distinct names
            extensions.stream()
                    .map(Extension::name)
                    .distinct()
                    .forEach(this::outln);
        } else {
            extensions.stream()
                    .map(Extension::toString)
                    .forEach(this::outln);
        }
    }

}