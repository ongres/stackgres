package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.commands.StackGresBaseCommand;
import picocli.CommandLine.Command;

@Command(name = "metrics", mixinStandardHelpOptions = true, subcommands = {CheckpointsMetricCommand.class}, description = "Inspect operational metrics for a PostgreSQL cluster")
public class MetricsCommand extends StackGresBaseCommand {
}