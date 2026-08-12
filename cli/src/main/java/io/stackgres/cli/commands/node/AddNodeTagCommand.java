package io.stackgres.cli.commands.node;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Command(name = "add", description = "Adds (or updates) tags on a PostgreSQL node")
public class AddNodeTagCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(index = "0", description = "The node ID", paramLabel = "<id>")
    UUID id;

    @Parameters(index = "1..*", arity = "1..*", split = ",", description = "Tags to add as key=value pairs", paramLabel = "<key=value>")
    Map<String, String> tags = new HashMap<>();

    @Override
    public void run() {
        if (debug) client.setDebug();
        Map<String, String> currentTags = client.addNodeTags(id, tags);
        outln("Tags updated on node " + id);
        if (currentTags.isEmpty())
            outln("Current tags: (none)");
        else
            outln("Current tags: " + new TreeMap<>(currentTags).entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")));
    }

}