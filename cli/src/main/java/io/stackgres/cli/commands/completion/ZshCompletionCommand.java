package io.stackgres.cli.commands.completion;

import io.stackgres.cli.commands.StackGresSubCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "zsh", description = "Generates zsh completion")
public class ZshCompletionCommand extends StackGresSubCommand {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        try {
            Completion completion = new Completion();
            outln(completion.getZshCompletion());
        } catch (Exception e) {
            errln("Could not generate completion, error: " + e.getMessage(), spec);
        }
    }
}
