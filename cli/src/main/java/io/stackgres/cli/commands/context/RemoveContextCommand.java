package io.stackgres.cli.commands.context;

import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.config.CliConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "remove", aliases = {"rm", "delete"}, description = "Removes a context")
public class RemoveContextCommand extends StackGresSubCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "The context name")
    String name;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        if (!config.remove(name)) {
            outln("No such context: " + name);
            return;
        }
        config.save();
        outln("Removed context '" + name + "'.");
    }
}
