package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.client.ExecSession;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresBaseCommand;
import io.stackgres.cli.exec.ExecTerminal;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Command(name = "exec", description = "Executes a command in a running PostgreSQL instance",
        showEndOfOptionsDelimiterInUsageHelp = true, customSynopsis = "@|bold stackgres cluster exec|@ [@|yellow -hX|@] @|yellow <name>|@ [--] @|yellow <command/args>|@...")
public class ExecInClusterCommand extends StackGresBaseCommand implements Callable<Integer> {

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(description = "The cluster name")
    String name;

    @Option(names = {"-i", "--instance"}, description = "The PostgreSQL instance name for high available clusters (default: the only instance or a random one)")
    String instanceName;

    @Parameters(index = "1..*", arity = "1..*", description = "The command and optional arguments", paramLabel = "<command/args>")
    List<String> args;

    @Override
    public Integer call() throws ExecutionException, InterruptedException {
        if (debug) client.setDebug();

        ExecTerminal terminal = new ExecTerminal();
        ExecSession session = client.execInCluster(name, instanceName, args, terminal.bytesPublisher(), terminal::write);
        return terminal.start(session.exitCode(), session::close);
    }

}