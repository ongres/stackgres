package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.LogFormatter;
import io.stackgres.cli.commands.StackGresSubCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Command(name = "logs", description = "Prints the logs of a running PostgreSQL instance")
public class ClusterLogsCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Spec
    CommandSpec spec;

    @Option(names = {"-f", "--follow"}, description = "Stream the logs")
    boolean follow;

    @Parameters(index = "0", description = "The cluster name")
    String name;

    @Option(names = {"-i", "--instance"}, description = "The PostgreSQL instance name for high available clusters (default: the only instance or a random one)")
    String instanceName;

    @Option(names = {"-c", "--component"}, description = "Log component: postgres, patroni, slon, etcd (default: postgres, or patroni for HA)")
    String component;

    @Option(names = {"--format"}, description = "Output format: colored (default), json, plain", defaultValue = "colored")
    String format;

    @Override
    public void run() {
        if (debug) client.setDebug();
        if (follow) {
            CompletableFuture<String> onCompleted = new CompletableFuture<>();
            Runnable runnable = client.followClusterLogs(name, instanceName, component,
                    line -> outln(LogFormatter.formatLine(line, format)), onCompleted::complete);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                runnable.run();
                try {
                    onCompleted.get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    errln("Timeout waiting for Matriarch connection shutdown", spec);
                }
            }));
            String error = onCompleted.join();
            if (error != null)
                throw new RuntimeException(error);
        } else
            out(LogFormatter.formatBlock(client.getClusterLogs(name, instanceName, component), format));
    }

}