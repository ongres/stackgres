package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.ClusterRow;
import io.stackgres.cli.postgres.EnvironmentInfo;
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

        // Fetch the environment list once: it decides whether to show the ENVIRONMENT column (only when
        // the endpoint exposes more than one — so a single-environment setup stays uncluttered) and feeds
        // the staleness hint below. Best-effort: never let it break the listing.
        List<EnvironmentInfo> environments;
        try {
            environments = client.listEnvironments();
        } catch (RuntimeException ignore) {
            environments = List.of();
        }
        boolean showEnv = environments.size() > 1;

        if (quiet) {
            rows.forEach(r -> outln(showEnv ? r.environmentId() + "/" + r.cluster().getName() : r.cluster().getName()));
            warnIfScopedStale(environment, environments);
            return;
        }
        if (rows.isEmpty()) {
            if (environment == null || environment.isBlank()) {
                outln("There are no PostgreSQL clusters in any environment");
            } else {
                outln("There are no PostgreSQL clusters on environment " + environment);
                warnIfScopedStale(environment, environments);
            }
            return;
        }

        int nameLen = Math.max(10, rows.stream().mapToInt(r -> r.cluster().getName().length()).max().orElse(8) + 2);

        StringBuilder fmt = new StringBuilder();
        fmt.append("%-").append(nameLen).append("s%-10s%-11s%-10s%-10s%-10s%-10s%-12s");
        if (showTags) fmt.append("%-10s");
        if (showEnv) fmt.append("%s");   // Environment: last column, unpadded (no trailing spaces)
        fmt.append("\n");
        String format = fmt.toString();

        List<Object> header = new ArrayList<>();
        header.addAll(List.of("Name", "Status", "Flavor", "Version", "Port", "Cores", "RAM", "DB Size"));
        if (showTags) header.add("Tags");
        if (showEnv) header.add("Environment");
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
            values.addAll(java.util.Arrays.asList(c.getName(), instance.getStatus(), flavor, instance.getVersion(), port, cores, ram, size));
            if (showTags) {
                values.add(c.getTags().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")));
            }
            if (showEnv) values.add(nvl(r.environmentId()));
            outf(format, values.toArray());
        }

        // A pinned environment can be disconnected/gone yet still serve CACHED clusters — so flag
        // staleness after a non-empty listing too, not only when it's empty. No-op for live envs.
        warnIfScopedStale(environment, environments);
    }

    /** Warn if a specific (non-aggregate) environment is scoped and turns out to be stale. */
    private void warnIfScopedStale(String environment, List<EnvironmentInfo> environments) {
        if (environment != null && !environment.isBlank()) {
            client.warnIfEnvironmentStale(environment, environments);
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

}
