package io.stackgres.cli.commands.environment;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.config.CliConfig;
import io.stackgres.cli.config.Context;
import io.stackgres.cli.postgres.EnvironmentInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Renames an environment. The name is the environment's identifier (what {@code -E} and
 * {@code environment use} take), unique per user; the cloud owns it, so this is meaningful against the
 * cloud — a local matriarch's name comes from its own configuration and it rejects a rename. Works
 * whether or not the environment is connected.
 *
 * <p>Any saved context of this CLI that pinned the old name (via {@code environment use}) is moved to
 * the new one, so the active environment keeps pointing at the same environment.
 */
@Command(name = "rename", description = "Renames an environment (its name is how it is addressed everywhere)")
public class RenameEnvironmentCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(index = "0", paramLabel = "<name>", description = "The environment's current name")
    String name;

    @Parameters(index = "1", paramLabel = "<new-name>",
            description = "The new name: lowercase letters and digits, with '-', '_' or '.' allowed in between (unique among your environments)")
    String newName;

    @Override
    public void run() {
        if (debug) client.setDebug();
        EnvironmentInfo renamed = client.renameEnvironment(name, newName);
        renameInContexts(name, renamed.name());
        outln("Environment '" + name + "' renamed to '" + renamed.name() + "'.");
    }

    /** Keep saved contexts that pinned the old name pointing at the same environment. */
    private void renameInContexts(String oldName, String renamedTo) {
        if (oldName.equals(renamedTo)) {
            return;
        }
        CliConfig config = CliConfig.load();
        boolean changed = false;
        for (Context c : config.contexts()) {
            if (oldName.equals(c.environment())) {
                config.upsert(new Context(c.name(), c.endpoint(), c.tls(), c.token(), renamedTo));
                changed = true;
            }
        }
        if (changed) {
            config.save();
        }
    }
}
