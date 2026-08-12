package io.stackgres.cli.commands.node;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Command(name = "remove", description = "Removes tags from a PostgreSQL node")
public class RemoveNodeTagCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(index = "0", description = "The node ID", paramLabel = "<id>")
    UUID id;

    @Parameters(index = "1..*", arity = "1..*", split = ",", description = "Tag keys to remove", paramLabel = "<key>")
    List<String> keys;

    @Override
    public void run() {
        if (debug) client.setDebug();
        Map<String, String> currentTags = client.removeNodeTags(id, keys);
        outln("Tags updated on node " + id);
        if (currentTags.isEmpty())
            outln("Tags: (none)");
        else
            outln("Tags: " + new TreeMap<>(currentTags).entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")));
    }

}