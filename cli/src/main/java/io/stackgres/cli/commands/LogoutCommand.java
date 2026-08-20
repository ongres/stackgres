package io.stackgres.cli.commands;

import io.stackgres.cli.config.CliConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Logs out by removing a saved context from {@code ~/.stackgres/config.yaml} — which deletes its stored
 * token. Defaults to the current context. A token supplied via the {@code STACKGRES_TOKEN} environment
 * variable is not affected (unset it to fully log out).
 */
@Command(name = "logout", description = "Logs out by removing a saved session (deletes its token)")
public class LogoutCommand extends StackGresSubCommand {

    @Parameters(index = "0", arity = "0..1", paramLabel = "<context>",
            description = "The context to remove (default: the current one)")
    String contextName;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        String name = contextName == null || contextName.isBlank() ? config.currentContext() : contextName.trim();
        if (name == null || name.isBlank()) {
            outln("Not logged in (no current context).");
            return;
        }
        if (!config.remove(name)) {
            outln("No such context: " + name);
            return;
        }
        config.save();
        outln("Logged out of '" + name + "'.");
        String envToken = System.getenv("STACKGRES_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            outln("Note: STACKGRES_TOKEN is still set in your environment — unset it to fully log out.");
        }
    }
}
