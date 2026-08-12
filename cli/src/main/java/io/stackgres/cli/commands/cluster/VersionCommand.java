package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.commands.StackGresBaseCommand;
import picocli.CommandLine.Command;

@Command(name = "version", mixinStandardHelpOptions = true,
        subcommands = {ListVersionsCommand.class},
        description = "Manages PostgreSQL versions")
public class VersionCommand extends StackGresBaseCommand {

}
