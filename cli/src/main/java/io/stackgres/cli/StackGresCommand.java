package io.stackgres.cli;

import io.stackgres.cli.commands.CliExecutionExceptionHandler;
import io.stackgres.cli.commands.LoginCommand;
import io.stackgres.cli.commands.LogoutCommand;
import io.stackgres.cli.commands.StackGresBaseCommand;
import io.stackgres.cli.commands.StatusCommand;
import io.stackgres.cli.commands.WhoAmICommand;
import io.stackgres.cli.commands.cluster.ClusterCommand;
import io.stackgres.cli.commands.completion.CompletionCommand;
import io.stackgres.cli.commands.context.ContextCommand;
import io.stackgres.cli.commands.environment.EnvironmentCommand;
import io.stackgres.cli.commands.node.NodeCommand;
import io.stackgres.cli.commands.slon.SlonCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "stackgres", mixinStandardHelpOptions = true, subcommands = {ClusterCommand.class, EnvironmentCommand.class, ContextCommand.class, NodeCommand.class, SlonCommand.class,
        LoginCommand.class, LogoutCommand.class, StatusCommand.class, WhoAmICommand.class, CompletionCommand.class/*, InfoCommand.class*/},
        description = "Manages StackGres (Own Your Database)")
public class StackGresCommand extends StackGresBaseCommand {

    public static void main(String... args) {
        // gRPC logs plaintext notices and name-resolution failures via java.util.logging at INFO/WARNING,
        // which would pollute clean command output (e.g. `status`). We surface reachability ourselves, so
        // quiet the library down to real errors. -X/--debug still shows our own stack traces.
        java.util.logging.Logger.getLogger("io.grpc").setLevel(java.util.logging.Level.SEVERE);
        int exitCode = new CommandLine(new StackGresCommand()).setExecutionExceptionHandler(new CliExecutionExceptionHandler()).execute(args);
        System.exit(exitCode);
    }

}