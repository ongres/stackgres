package io.stackgres.cli.commands.context;

import io.stackgres.cli.commands.StackGresBaseCommand;
import picocli.CommandLine.Command;

@Command(name = "context", mixinStandardHelpOptions = true, subcommands = {ListContextCommand.class, CurrentContextCommand.class, UseContextCommand.class, SetContextCommand.class, DeleteContextCommand.class},
        description = "Manages connection contexts to local StackGres installations and to the cloud")
public class ContextCommand extends StackGresBaseCommand {

}