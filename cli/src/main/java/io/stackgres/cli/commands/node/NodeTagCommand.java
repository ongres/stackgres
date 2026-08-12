package io.stackgres.cli.commands.node;

import io.stackgres.cli.commands.StackGresBaseCommand;
import picocli.CommandLine.Command;

@Command(name = "tag", mixinStandardHelpOptions = true, subcommands = {AddNodeTagCommand.class, RemoveNodeTagCommand.class}, description = "Manages tags on PostgreSQL nodes")
public class NodeTagCommand extends StackGresBaseCommand {

}
