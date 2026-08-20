package io.stackgres.cli.commands.context;

import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.config.CliConfig;
import picocli.CommandLine.Command;

@Command(name = "current", description = "Shows the current context name")
public class CurrentContextCommand extends StackGresSubCommand {

    @Override
    public void run() {
        String current = CliConfig.load().currentContext();
        if (current == null || current.isBlank()) {
            outln("No current context set.");
            return;
        }
        outln(current);
    }
}
