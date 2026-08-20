package io.stackgres.cli.commands.environment;

import io.stackgres.cli.commands.StackGresBaseCommand;
import picocli.CommandLine.Command;

@Command(name = "environment", aliases = {"env"}, mixinStandardHelpOptions = true, subcommands = {ListEnvironmentCommand.class, GetEnvironmentCommand.class, UseEnvironmentCommand.class, DeleteEnvironmentCommand.class},
        description = "Lists, inspects, selects, and prunes environments (a local matriarch, or the cloud fleet)")
public class EnvironmentCommand extends StackGresBaseCommand {

}