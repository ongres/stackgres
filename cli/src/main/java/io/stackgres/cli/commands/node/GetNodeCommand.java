package io.stackgres.cli.commands.node;

import io.stackgres.cli.Strings;
import io.stackgres.cli.Times;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.Slony;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.UUID;
import java.util.stream.Collectors;

@Command(name = "get", description = "Get a PostgreSQL node by its ID")
public class GetNodeCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(description = "The node ID")
    UUID id;

    @Override
    public void run() {
        if (debug) client.setDebug();
        Slony slony = client.listSlonys().stream()
                .filter(s -> s.id().equals(id))
                .findAny().orElse(null);

        if (slony == null)
            throw new IllegalArgumentException("No node found with ID: " + id);

        // Which environment the node lives in — only when several exist (single-env stays uncluttered).
        boolean multiEnv;
        try {
            multiEnv = client.listEnvironments().size() > 1;
        } catch (RuntimeException ignore) {
            multiEnv = false;
        }
        String envLine = multiEnv ? "\nEnvironment:  " + slony.environmentId() : "";

        String cloud = "";
        if (slony.cloudEnvironment() != null) {
            String region = slony.cloudEnvironment().region() != null ? slony.cloudEnvironment().region() : "N/A";
            String az = slony.cloudEnvironment().availabilityZone() != null ? slony.cloudEnvironment().availabilityZone() : "N/A";
            String computeInstance = slony.cloudEnvironment().computeInstanceName() != null ? slony.cloudEnvironment().computeInstanceName() : "N/A";
            cloud = """

                    Cloud:        $cloud$
                    Region:       $region$
                    AZ:           $az$
                    Instance:     $computeInstance$"""
                    .replace("$cloud$", slony.cloudEnvironment().cloud().id())
                    .replace("$region$", region)
                    .replace("$az$", az)
                    .replace("$computeInstance$", computeInstance);
        }

        String tags = "";
        if (slony.tags() != null && !slony.tags().isEmpty()) {
            String tagList = slony.tags().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
            tags = "\nTags:         " + tagList;
        }

        String output = """
                PostgreSQL node:

                ID:           $id$$env$
                Hostname:     $hostname$
                OS:           $os$
                Arch:         $arch$
                Version:      $version$
                CPUs:         $cpu$
                Memory:       $memory$
                Status:       $status$
                Last seen:    $lastSeen$$tags$$cloud$"""
                .replace("$id$", slony.id().toString())
                .replace("$env$", envLine)
                .replace("$hostname$", slony.hostname())
                .replace("$os$", slony.os())
                .replace("$arch$", slony.arch())
                .replace("$version$", slony.version())
                .replace("$cpu$", String.valueOf(slony.cpu()))
                .replace("$memory$", Strings.formatMemory(slony.memory()))
                .replace("$status$", String.valueOf(slony.status()))
                .replace("$lastSeen$", slony.lastHeartbeat() == null ? "Never" : Times.stampAndAgo(slony.lastHeartbeat()))
                .replace("$tags$", tags)
                .replace("$cloud$", cloud);
        outln(output);
    }

}