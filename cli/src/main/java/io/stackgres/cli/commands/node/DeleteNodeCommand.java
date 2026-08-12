package io.stackgres.cli.commands.node;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.InteractivePrompt;
import io.stackgres.cli.commands.StackGresPicocliException;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.commands.ProgressMessages;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.util.UUID;

@Command(name = "delete", description = "Removes a PostgreSQL node from the Matriarch registry")
public class DeleteNodeCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Spec
    CommandSpec spec;

    @Parameters(description = "The node ID")
    UUID id;

    @Option(names = {"-f", "--force"}, description = "Force deletion (doesn't ask for confirmation)")
    boolean force;

    @Override
    public void run() {
        if (!force) {
            outln("This will remove node " + id + " from Matriarch");
            if (!new InteractivePrompt(spec.commandLine()).confirm("delete"))
                throw new CommandLine.PicocliException("Aborted");
        }

        ProgressMessages messages = new ProgressMessages(spec.commandLine());
        if (debug) client.setDebug(messages);
        try {
            client.deleteSlony(id, hostname -> messages.doneAddFirstLine("The node " + hostname + " has been removed"));
        } catch (Exception e) {
            throw new StackGresPicocliException(e, messages);
        }
    }

}