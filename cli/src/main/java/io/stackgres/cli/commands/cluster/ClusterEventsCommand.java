package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.proto.cli.Event;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Command(name = "events", description = "Get events for a PostgreSQL cluster", hidden = true)
public class ClusterEventsCommand extends StackGresSubCommand {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(description = "The cluster name")
    String name;

    @Override
    public void run() {
        if (debug) client.setDebug();
        List<Event> events = client.getClusterEvents(name);

        for (Event event : events) {
            String time = FORMATTER.format(Instant.ofEpochMilli(event.getTimestamp()));
            StringBuilder data = new StringBuilder();
            if (!"CLUSTER".equals(event.getScope()))
                data.append("  instanceId=").append(event.getScopeId().getValue().toStringUtf8());
            event.getDataMap().forEach((k, v) -> data.append("  ").append(k).append("=").append(v));
            outln(String.format("%-19s  %-35s%s", time, event.getType(), data));
        }
    }

}
