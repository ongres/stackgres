package io.stackgres.cli.commands.environment;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.InteractivePrompt;
import io.stackgres.cli.commands.ProgressMessages;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.config.CliConfig;
import io.stackgres.cli.config.Context;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.util.List;

/**
 * Prunes decommissioned environments from the cloud: their cached clusters and entries are dropped from
 * the aggregated view. Only for DISCONNECTED environments — the cloud refuses while one is still connected
 * (stop the matriarch first), so a connected id in a batch simply reports a failure and the rest proceed.
 * Meaningful against the cloud; a local matriarch is its own single environment and rejects it.
 */
@Command(name = "delete", description = "Deletes one or more disconnected environments from the cloud (prunes their cached clusters)")
public class DeleteEnvironmentCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Spec
    CommandLine.Model.CommandSpec spec;

    @Parameters(arity = "1..*", paramLabel = "<id>", description = "One or more environment ids (must be disconnected)")
    List<String> ids;

    @Option(names = {"-f", "--force"}, description = "Force deletion (doesn't ask for confirmation)")
    boolean force;

    @Override
    public void run() {
        ProgressMessages messages = new ProgressMessages(spec.commandLine());
        if (debug) client.setDebug(messages);
        if (!force) {
            outln("This will remove environment(s) " + String.join(", ", ids) + " and their cached clusters from the cloud view.");
            if (!new InteractivePrompt(spec.commandLine()).confirm("delete"))
                throw new CommandLine.PicocliException("Aborted");
        }
        runBatch(ids, messages, "environment", "deleted", id -> {
            client.deleteEnvironment(id);
            clearFromContexts(id);
        });
    }

    /**
     * Drop a just-deleted environment from any saved context that pinned it (via {@code environment
     * use}), so subsequent commands fall back to "all environments" instead of a now-deleted one.
     */
    private void clearFromContexts(String id) {
        CliConfig config = CliConfig.load();
        boolean cleared = false;
        for (Context c : config.contexts()) {
            if (id.equals(c.environment())) {
                config.upsert(new Context(c.name(), c.endpoint(), c.tls(), c.token(), null));
                cleared = true;
            }
        }
        if (cleared) {
            config.save();
        }
    }

}
