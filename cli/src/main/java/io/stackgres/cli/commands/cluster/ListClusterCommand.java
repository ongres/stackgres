package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.postgres.ClusterInstance;
import io.stackgres.postgres.Flavor;
import io.stackgres.postgres.PostgresCluster;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "list", description = "Lists the PostgreSQL clusters", usageHelpWidth = 160)
public class ListClusterCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Option(names = {"--show-tags"}, description = "Shows the cluster tags in the list")
    boolean showTags;

    @Option(names = {"-q", "--quiet"}, description = "Only display the cluster names")
    boolean quiet;

    @Option(names = {"-t", "--tag"}, description = "Only list clusters that are tagged accordingly", split = ",", paramLabel = "<key=value>")
    Map<String, String> tags = new HashMap<>();

    @Override
    public void run() {
        if (debug) client.setDebug();
        List<PostgresCluster> clusters = client.listClusters(tags);

        if (quiet) {
            clusters.stream().map(PostgresCluster::getName).forEach(this::outln);
            return;
        }

        if (clusters.isEmpty()) {
            outln("There are no PostgreSQL clusters running");
            return;
        }

        int nameMaxLength = clusters.stream().map(PostgresCluster::getName).mapToInt(String::length).max().orElseThrow();
        int nameLength = (nameMaxLength <= 8) ? 10 : (nameMaxLength + 2);

        if (showTags)
            outf("%-" + nameLength + "s%-10s%-11s%-10s%-10s%-10s%-10s%-12s%-10s\n", "Name", "Status", "Flavor", "Version", "Port", "Cores", "RAM", "DB Size", "Tags");
        else
            outf("%-" + nameLength + "s%-10s%-11s%-10s%-10s%-10s%-10s%-12s\n", "Name", "Status", "Flavor", "Version", "Port", "Cores", "RAM", "DB Size");
        clusters.forEach(c -> {
            ClusterInstance instance = c.getInstances().iterator().next();
            String port = instance.getPort() != null ? String.valueOf(instance.getPort()) : "N/A";
            String cores = c.getCpu() > 0 ? formatCpu(c.getCpu()) : "N/A";
            String ram = c.getMemory() > 0 ? formatBytes(c.getMemory()) : "N/A";
            String size = c.getDbSize() > 0 ? formatBytes(c.getDbSize()) : "N/A";
            String flavor = (c.getFlavor() != null ? c.getFlavor() : Flavor.POSTGRES).toString();
            if (showTags) {
                String tags = c.getTags().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
                outf("%-" + nameLength + "s%-10s%-11s%-10s%-10s%-10s%-10s%-12s%-10s\n", c.getName(), instance.getStatus(), flavor, instance.getVersion(), port, cores, ram, size, tags);
            } else
                outf("%-" + nameLength + "s%-10s%-11s%-10s%-10s%-10s%-10s%-12s\n", c.getName(), instance.getStatus(), flavor, instance.getVersion(), port, cores, ram, size);
        });
    }

}