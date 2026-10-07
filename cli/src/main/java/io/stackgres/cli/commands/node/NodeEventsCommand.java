package io.stackgres.cli.commands.node;

import io.stackgres.cli.Times;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.proto.types.v1.Event;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Command(name = "events", description = "Get events for a node", hidden = true)
public class NodeEventsCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(description = "The node ID")
    UUID id;

    @Override
    public void run() {
        if (debug) client.setDebug();
        List<Event> events = client.getNodeEvents(id);

        for (Event event : events) {
            String time = Times.stamp(Instant.ofEpochSecond(event.getTimestamp().getSeconds(), event.getTimestamp().getNanos()));
            StringBuilder data = new StringBuilder();
            event.getDataMap().forEach((k, v) -> data.append("  ").append(k).append("=").append(v));
            outln(String.format("%-23s  %-35s%s", time, event.getType(), data));
        }
    }

}
