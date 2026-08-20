package io.stackgres.cli.commands;

import io.stackgres.cli.CliContext;
import picocli.CommandLine.Option;

public abstract class StackGresBaseCommand {

    @Option(names = {"-h", "--help"}, usageHelp = true)
    protected boolean help;

    @Option(names = {"-X", "--debug"}, description = "Prints debug and error stacktrace information", hidden = true)
    protected boolean debug;

    @Option(names = {"-E", "--environment"}, paramLabel = "<id>", description = "The target environment id (default: $STACKGRES_ENVIRONMENT, the context's environment, or 'local'). Use 'stackgres environment list' to see available ids.")
    public void setEnvironment(String environment) {
        CliContext.setEnvironment(environment);
    }

    @Option(names = "--context", paramLabel = "<name>", description = "Use a named context from ~/.stackgres/config.yaml instead of the current one.")
    public void setContext(String context) {
        CliContext.setContext(context);
    }

}