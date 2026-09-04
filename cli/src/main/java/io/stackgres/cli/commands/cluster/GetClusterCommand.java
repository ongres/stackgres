package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.Times;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.ClusterRow;
import io.stackgres.cli.postgres.EnvironmentInfo;
import io.stackgres.postgres.ClusterInstance;
import io.stackgres.postgres.Extension;
import io.stackgres.postgres.Flavor;
import io.stackgres.postgres.PostgresCluster;
import io.stackgres.postgres.SlonyLinuxInstance;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Command(name = "get", description = "Get a PostgreSQL clusters by its name")
public class GetClusterCommand extends StackGresSubCommand {

    private static final Comparator<ClusterInstance> INSTANCE_COMPARATOR = Comparator.comparing(ClusterInstance::getName);

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(description = "The cluster name")
    String name;

    @Option(names = "--instances-only", description = "Only display the cluster instance names", hidden = true)
    boolean instancesOnly;

    @Override
    public void run() {
        if (debug) client.setDebug();
        ClusterRow row;
        try {
            row = client.resolveCluster(name);
        } catch (RuntimeException e) {
            // A miss may just mean the active environment is stale/disconnected — hint at it (read
            // path, no spinner, so a direct warning renders cleanly).
            client.warnIfEnvironmentStale(client.configuredEnvironment());
            throw e;
        }
        PostgresCluster cluster = row.cluster();

        if (instancesOnly) {
            cluster.getInstances().stream()
                    .sorted(INSTANCE_COMPARATOR)
                    .map(ClusterInstance::getName)
                    .forEach(this::outln);
            return;
        }

        String tags = cluster.getTags().isEmpty() ? "" : "\nTags:         " + cluster.getTags().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
        String extensions = cluster.getExtensions().isEmpty() ? "" : "\nExtensions:   " + cluster.getExtensions().stream().map(Extension::toString).collect(Collectors.joining(", "));

        String instances = cluster.getInstances().stream()
                .sorted(INSTANCE_COMPARATOR)
                .map(i -> {
                    String slony = (i instanceof SlonyLinuxInstance) ? "\nNode ID:      " + ((SlonyLinuxInstance) i).getSlonyId().toString() : "";
                    String port = i.getPort() != null ? String.valueOf(i.getPort()) : "N/A";
                    String ivorySqlPort = (cluster.getFlavor() == Flavor.IVORY_SQL && i.getIvorySqlPort() != null)
                            ? "\nIvorySQL Port: " + i.getIvorySqlPort() : "";
                    String cores = i.getCpu() > 0 ? "\nCores:        " + formatCpu(i.getCpu()) : "";
                    String ram = i.getMemory() > 0 ? "\nRAM:          " + formatBytes(i.getMemory()) : "";
                    String size = i.getDbSize() > 0 ? "\nDB Size:      " + formatBytes(i.getDbSize()) : "";
                    return """


                                    Instance:

                                    Name:         $name$
                                    ID:           $id$
                                    Status:       $status$
                                    Replication:  $replication$
                                    Version:      $version$
                                    Address:      $address$
                                    Port:         $port$$ivorySqlPort$$cores$$ram$$size$$slony$
                                    """
                                    .replace("$name$", i.getName())
                                    .replace("$id$", i.getId().toString())
                                    .replace("$status$", i.getStatus().toString())
                                    .replace("$replication$", i.getReplicationStatus().toString())
                                    .replace("$version$", i.getVersion())
                                    .replace("$port$", port)
                                    .replace("$ivorySqlPort$", ivorySqlPort)
                                    .replace("$address$", i.getExternalAddress())
                                    .replace("$cores$", cores)
                                    .replace("$ram$", ram)
                                    .replace("$size$", size)
                                    .replace("$slony$", slony);
                })
                .collect(Collectors.joining());

        // Extra topology only when it helps: which environment (when several exist) and — when that
        // environment is disconnected, so the status above reads Unknown — when we last heard from it.
        List<EnvironmentInfo> environments;
        try {
            environments = client.listEnvironments();
        } catch (RuntimeException ignore) {
            environments = List.of();
        }
        EnvironmentInfo env = environments.stream()
                .filter(e -> e.id().equals(row.environmentId())).findFirst().orElse(null);
        boolean multiEnv = environments.size() > 1;
        boolean stale = env != null
                && ("Disconnected".equalsIgnoreCase(env.health()) || "Cached".equalsIgnoreCase(env.source()));
        String envLine = multiEnv ? "\nEnvironment:  " + row.environmentId() : "";
        String lastSeen = stale ? "\nLast seen:    " + Times.stampAndAgo(env.asOf()) : "";

        Flavor flavor = cluster.getFlavor() != null ? cluster.getFlavor() : Flavor.POSTGRES;
        String output = """
                PostgreSQL cluster:

                Name:         $name$
                ID:           $id$
                Flavor:       $flavor$$env$$lastSeen$$tags$$extensions$$instances$"""
                .replace("$name$", cluster.getName())
                .replace("$id$", cluster.getId().toString())
                .replace("$flavor$", flavor.toString())
                .replace("$env$", envLine)
                .replace("$lastSeen$", lastSeen)
                .replace("$tags$", tags)
                .replace("$extensions$", extensions)
                .replace("$instances$", instances);

        outln(output);
    }

}