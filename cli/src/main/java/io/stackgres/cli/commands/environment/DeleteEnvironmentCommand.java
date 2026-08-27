package io.stackgres.cli.commands.environment;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.InteractivePrompt;
import io.stackgres.cli.commands.StackGresSubCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * Prunes a decommissioned environment from the cloud: its cached clusters and its entry are dropped from
 * the aggregated view. Only for a DISCONNECTED environment — the cloud refuses while it is still
 * connected (stop the matriarch first). Meaningful against the cloud; a local matriarch is its own single
 * environment and rejects it.
 */
@Command(name = "delete", description = "Deletes a disconnected environment from the cloud (prunes its cached clusters)")
public class DeleteEnvironmentCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Spec
    CommandLine.Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "<id>", description = "The environment id (must be disconnected)")
    String id;

    @Option(names = {"-f", "--force"}, description = "Force deletion (doesn't ask for confirmation)")
    boolean force;

    @Override
    public void run() {
        if (debug) client.setDebug();
        if (!force) {
            outln("This will remove environment '" + id + "' and its cached clusters from the cloud view.");
            if (!new InteractivePrompt(spec.commandLine()).confirm("delete"))
                throw new CommandLine.PicocliException("Aborted");
        }
        client.deleteEnvironment(id);
        outln("Environment '" + id + "' deleted.");
    }

}