package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.ClusterRow;
import io.stackgres.postgres.ClusterInstance;
import io.stackgres.postgres.Flavor;
import io.stackgres.postgres.PostgresCluster;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "list", description = "Lists the PostgreSQL clusters")
public class ListClusterCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Option(names = {"--show-tags"}, description = "Shows the cluster tags in the list")
    boolean showTags;

    @Option(names = {"-q", "--quiet"}, description = "Only display the cluster names")
    boolean quiet;

    @Option(names = {"-A", "--all-environments"}, description = "List across all environments (the default when no environment is active)")
    boolean allEnvironments;

    @Option(names = {"-t", "--tag"}, description = "Only list clusters that are tagged accordingly", split = ",", paramLabel = "<key=value>")
    Map<String, String> tags = new HashMap<>();

    @Override
    public void run() {
        if (debug) client.setDebug();
        // Active environment scopes the list; unset (or -A) aggregates across all environments.
        String environment = allEnvironments ? "" : client.configuredEnvironment();
        List<ClusterRow> rows = client.listClusterRows(environment, tags);

        // Show the ENVIRONMENT column only when the listing actually spans more than one environment.
        boolean showEnv = rows.stream().map(ClusterRow::environmentId)
                .filter(s -> s != null && !s.isBlank()).distinct().count() > 1;

        if (quiet) {
            rows.forEach(r -> outln(showEnv ? r.environmentId() + "/" + r.cluster().getName() : r.cluster().getName()));
            return;
        }
        if (rows.isEmpty()) {
            outln("There are no PostgreSQL clusters running");
            return;
        }

        int nameLen = Math.max(10, rows.stream().mapToInt(r -> r.cluster().getName().length()).max().orElse(8) + 2);
        int envLen = showEnv ? Math.max(13, rows.stream().mapToInt(r -> nvl(r.environmentId()).length()).max().orElse(0) + 2) : 0;

        StringBuilder fmt = new StringBuilder();
        if (showEnv) fmt.append("%-").append(envLen).append("s");
        fmt.append("%-").append(nameLen).append("s%-10s%-11s%-10s%-10s%-10s%-10s%-12s");
        if (showTags) fmt.append("%-10s");
        fmt.append("\n");
        String format = fmt.toString();

        List<Object> header = new ArrayList<>();
        if (showEnv) header.add("ENVIRONMENT");
        header.addAll(List.of("Name", "Status", "Flavor", "Version", "Port", "Cores", "RAM", "DB Size"));
        if (showTags) header.add("Tags");
        outf(format, header.toArray());

        for (ClusterRow r : rows) {
            PostgresCluster c = r.cluster();
            ClusterInstance instance = c.getInstances().iterator().next();
            String port = instance.getPort() != null ? String.valueOf(instance.getPort()) : "N/A";
            String cores = c.getCpu() > 0 ? formatCpu(c.getCpu()) : "N/A";
            String ram = c.getMemory() > 0 ? formatBytes(c.getMemory()) : "N/A";
            String size = c.getDbSize() > 0 ? formatBytes(c.getDbSize()) : "N/A";
            String flavor = (c.getFlavor() != null ? c.getFlavor() : Flavor.POSTGRES).toString();
            List<Object> values = new ArrayList<>();
            if (showEnv) values.add(nvl(r.environmentId()));
            values.addAll(java.util.Arrays.asList(c.getName(), instance.getStatus(), flavor, instance.getVersion(), port, cores, ram, size));
            if (showTags) {
                values.add(c.getTags().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")));
            }
            outf(format, values.toArray());
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

}
