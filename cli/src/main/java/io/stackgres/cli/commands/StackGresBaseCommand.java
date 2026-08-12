package io.stackgres.cli.commands;

import picocli.CommandLine.Option;

public abstract class StackGresBaseCommand {

    @Option(names = {"-h", "--help"}, usageHelp = true)
    protected boolean help;

    @Option(names = {"-X", "--debug"}, description = "Prints debug and error stacktrace information", hidden = true)
    protected boolean debug;

}
