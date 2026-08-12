package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.commands.*;
import picocli.CommandLine.Command;

@Command(name = "cluster",
        mixinStandardHelpOptions = true,
        subcommands = {ListClusterCommand.class, GetClusterCommand.class, CreateClusterCommand.class, DeleteClusterCommand.class, StartClusterCommand.class,
                StopClusterCommand.class, RestartClusterCommand.class, ExtensionCommand.class, VersionCommand.class, PsqlCommand.class,
                ExecInClusterCommand.class, ClusterLogsCommand.class, TunnelCommand.class, DiagnosticsClusterCommand.class, ClusterEventsCommand.class,
                MetricsCommand.class},
        description = "Manages PostgreSQL clusters (StackGres)")
public class ClusterCommand extends StackGresBaseCommand {

}