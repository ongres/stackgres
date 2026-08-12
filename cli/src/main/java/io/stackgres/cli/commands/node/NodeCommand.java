package io.stackgres.cli.commands.node;

import io.stackgres.cli.commands.StackGresBaseCommand;
import picocli.CommandLine.Command;

@Command(name = "node", mixinStandardHelpOptions = true, subcommands = {ListNodeCommand.class, GetNodeCommand.class, DeleteNodeCommand.class,
        NodeTagCommand.class, NodeLogsCommand.class, NodeEventsCommand.class}, description = "Manages PostgreSQL node resources")
public class NodeCommand extends StackGresBaseCommand {

}