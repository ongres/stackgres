package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.commands.StackGresBaseCommand;
import picocli.CommandLine.Command;

@Command(name = "extension", mixinStandardHelpOptions = true, subcommands = {ListExtensionsSubCommand.class}, description = "Manages PostgreSQL extensions")
public class ExtensionCommand extends StackGresBaseCommand {

}
