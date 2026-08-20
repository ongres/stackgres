package io.stackgres.cli.commands.context;

import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.config.CliConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "use", description = "Sets the current context")
public class UseContextCommand extends StackGresSubCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "The context name")
    String name;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        config.use(name);   // IllegalArgumentException (handled) if it doesn't exist
        config.save();
        outln("Switched to context '" + name + "'.");
    }
}
