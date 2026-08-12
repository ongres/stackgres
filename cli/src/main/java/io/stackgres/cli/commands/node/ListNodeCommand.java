package io.stackgres.cli.commands.node;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.Slony;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "list", description = "Lists the PostgreSQL nodes", usageHelpWidth = 160)
public class ListNodeCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Option(names = {"-q", "--quiet"}, description = "Only display the node IDs")
    boolean quiet;

    @Option(names = {"--show-tags"}, description = "Shows the node tags in the list")
    boolean showTags;

    @Option(names = {"-t", "--tag"}, description = "Only list nodes that are tagged accordingly", split = ",", paramLabel = "<key=value>")
    Map<String, String> tags = new HashMap<>();

    @Override
    public void run() {
        if (debug) client.setDebug();
        List<Slony> slonys = client.listSlonys(tags);

        if (quiet) {
            slonys.stream().map(slony -> slony.id().toString()).forEach(this::outln);
            return;
        }

        if (slonys.isEmpty()) {
            outln("There are no PostgreSQL nodes");
            return;
        }

        int nameMaxLength = slonys.stream().map(Slony::hostname).mapToInt(String::length).max().orElseThrow();
        int nameLength = (nameMaxLength <= 8) ? 10 : (nameMaxLength + 2);
        int osMaxLength = slonys.stream().map(Slony::os).mapToInt(String::length).max().orElseThrow();
        int osLength = (osMaxLength <= 8) ? 10 : (osMaxLength + 2);

        if (showTags)
            outf("%-38s%-" + nameLength + "s%-" + osLength + "s%-10s%-15s%-10s\n", "ID", "Hostname", "OS", "CPUs", "Status", "Tags");
        else
            outf("%-38s%-" + nameLength + "s%-" + osLength + "s%-10s%-15s\n", "ID", "Hostname", "OS", "CPUs", "Status");
        slonys.forEach(c -> {
            if (showTags) {
                String tagStr = c.tags().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
                outf("%-38s%-" + nameLength + "s%-" + osLength + "s%-10s%-15s%-10s\n", c.id(), c.hostname(), c.os(), c.cpu(), c.status(), tagStr);
            } else
                outf("%-38s%-" + nameLength + "s%-" + osLength + "s%-10s%-15s\n", c.id(), c.hostname(), c.os(), c.cpu(), c.status());
        });
    }

}