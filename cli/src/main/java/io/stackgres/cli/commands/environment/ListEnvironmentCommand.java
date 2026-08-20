package io.stackgres.cli.commands.environment;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.EnvironmentInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Command(name = "list", description = "Lists the environments", usageHelpWidth = 160)
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

        int idLen = width(environments, EnvironmentInfo::id, "ID", 12);
        int kindLen = width(environments, EnvironmentInfo::kind, "KIND", 14);
        String fmt = "%-" + idLen + "s%-" + kindLen + "s%-10s%-16s%-22s\n";
        outf(fmt, "ID", "KIND", "SOURCE", "HEALTH", "AS-OF");
        environments.forEach(e -> outf(fmt, e.id(), e.kind(), e.source(), e.health(), e.asOf() == null ? "" : e.asOf().toString()));
    }

    private static int width(List<EnvironmentInfo> rows, Function<EnvironmentInfo, String> field, String header, int min) {
        int max = rows.stream().map(field).filter(Objects::nonNull).mapToInt(String::length).max().orElse(0);
        return Math.max(Math.max(max, header.length()), min) + 2;
    }

}