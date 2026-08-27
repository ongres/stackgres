package io.stackgres.cli.commands.environment;

import io.stackgres.cli.commands.StackGresBaseCommand;
import io.stackgres.cli.commands.node.NodeCommand;
import picocli.CommandLine.Command;

@Command(name = "environment", aliases = {"env"}, mixinStandardHelpOptions = true,
        subcommands = {ListEnvironmentCommand.class, GetEnvironmentCommand.class, UseEnvironmentCommand.class,
                DeleteEnvironmentCommand.class, NodeCommand.class},
        description = "Lists, inspects, selects, and prunes environments (a local matriarch, or the cloud fleet)")
public class EnvironmentCommand extends StackGresBaseCommand {

}