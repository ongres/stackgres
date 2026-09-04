package io.stackgres.cli.commands.node;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.Slony;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "list", description = "Lists the PostgreSQL nodes")
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

        // ENVIRONMENT column only when the endpoint exposes more than one (keeps single-env output clean).
        boolean multiEnv;
        try {
            multiEnv = client.listEnvironments().size() > 1;
        } catch (RuntimeException ignore) {
            multiEnv = false;
        }
        final boolean showEnv = multiEnv;

        int nameMaxLength = slonys.stream().map(Slony::hostname).mapToInt(String::length).max().orElseThrow();
        int nameLength = (nameMaxLength <= 8) ? 10 : (nameMaxLength + 2);
        int osMaxLength = slonys.stream().map(Slony::os).mapToInt(String::length).max().orElseThrow();
        int osLength = (osMaxLength <= 8) ? 10 : (osMaxLength + 2);

        String format = "%-38s%-" + nameLength + "s%-" + osLength + "s%-10s%-15s"
                + (showTags ? "%-10s" : "")
                + (showEnv ? "%s" : "")   // Environment: last column, unpadded (no trailing spaces)
                + "\n";

        List<Object> header = new ArrayList<>();
        header.addAll(List.of("ID", "Hostname", "OS", "CPUs", "Status"));
        if (showTags) header.add("Tags");
        if (showEnv) header.add("Environment");
        outf(format, header.toArray());

        slonys.forEach(c -> {
            List<Object> values = new ArrayList<>();
            values.addAll(Arrays.asList(c.id(), c.hostname(), c.os(), c.cpu(), c.status()));
            if (showTags)
                values.add(c.tags().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")));
            if (showEnv) values.add(nvl(c.environmentId()));
            outf(format, values.toArray());
        });
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

}