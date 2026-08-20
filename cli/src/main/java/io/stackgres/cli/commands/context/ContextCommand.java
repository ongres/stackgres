package io.stackgres.cli.commands.context;

import io.stackgres.cli.commands.StackGresBaseCommand;
import picocli.CommandLine.Command;

@Command(name = "context", aliases = {"ctx"}, mixinStandardHelpOptions = true,
        subcommands = {ListContextCommand.class, CurrentContextCommand.class, UseContextCommand.class,
                SetContextCommand.class, RemoveContextCommand.class},
        description = "Manages connection contexts (~/.stackgres/config.yaml) — local matriarch(s) and the cloud")
public class ContextCommand extends StackGresBaseCommand {

}
