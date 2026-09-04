package io.stackgres.cli;

import io.stackgres.cli.commands.*;
import io.stackgres.cli.commands.cluster.ClusterCommand;
import io.stackgres.cli.commands.completion.CompletionCommand;
import io.stackgres.cli.commands.context.ContextCommand;
import io.stackgres.cli.commands.environment.EnvironmentCommand;
import io.stackgres.cli.commands.slon.SlonCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "stackgres", subcommands = {ClusterCommand.class, EnvironmentCommand.class, ContextCommand.class, SlonCommand.class,
        LoginCommand.class, LogoutCommand.class, StatusCommand.class, VersionCommand.class, CompletionCommand.class/*, InfoCommand.class*/}, description = "Manages StackGres (stackgres.io)")
public class StackGresCommand extends StackGresBaseCommand {

    public static void main(String... args) {
        // gRPC logs plaintext notices and name-resolution failures via java.util.logging at INFO/WARNING,
        // which would pollute clean command output (e.g. `status`). We surface reachability ourselves, so
        // quiet the library down to real errors. -X/--debug still shows our own stack traces.
        java.util.logging.Logger.getLogger("io.grpc").setLevel(java.util.logging.Level.SEVERE);
        int exitCode = new CommandLine(new StackGresCommand())
                .setExecutionExceptionHandler(new CliExecutionExceptionHandler())
                // Wider help for every (sub)command — setUsageHelpWidth propagates recursively, so this
                // replaces the per-command `usageHelpWidth = 160`. Nothing else needs to set it.
                .setUsageHelpWidth(160)
                .execute(args);
        System.exit(exitCode);
    }

}