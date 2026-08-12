package io.stackgres.cli;

import io.stackgres.cli.commands.*;
import io.stackgres.cli.commands.cluster.ClusterCommand;
import io.stackgres.cli.commands.completion.CompletionCommand;
import io.stackgres.cli.commands.node.NodeCommand;
import io.stackgres.cli.commands.slon.SlonCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "stackgres", mixinStandardHelpOptions = true, subcommands = {ClusterCommand.class, NodeCommand.class, SlonCommand.class, WhoAmICommand.class, CompletionCommand.class/*, InfoCommand.class*/},
        description = "Manages StackGres (Own Your Database)")
public class StackGresCommand extends StackGresBaseCommand {

    public static void main(String... args) {
        int exitCode = new CommandLine(new StackGresCommand()).setExecutionExceptionHandler(new CliExecutionExceptionHandler()).execute(args);
        System.exit(exitCode);
    }

}