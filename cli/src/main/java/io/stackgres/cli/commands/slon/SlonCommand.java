package io.stackgres.cli.commands.slon;

import io.stackgres.cli.commands.StackGresBaseCommand;
import picocli.CommandLine.Command;

@Command(name = "slon", mixinStandardHelpOptions = true, subcommands = {ListSlonCommand.class, GetSlonCommand.class}, description = "Manages PostgreSQL Slon (StackGres) resources", hidden = true)
public class SlonCommand extends StackGresBaseCommand {

}