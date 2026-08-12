package io.stackgres.cli.commands.node;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.LogFormatter;
import io.stackgres.cli.commands.StackGresSubCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Command(name = "logs", description = "Prints the logs of a node", hidden = true)
public class NodeLogsCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Spec
    CommandSpec spec;

    @Option(names = {"-f", "--follow"}, description = "Stream the logs")
    boolean follow;

    @Parameters(description = "The node ID")
    UUID id;

    @Option(names = {"--format"}, description = "Output format: colored (default), json, plain", defaultValue = "colored")
    String format;

    @Override
    public void run() {
        if (debug) client.setDebug();
        if (follow) {
            CompletableFuture<String> onCompleted = new CompletableFuture<>();
            Runnable runnable = client.followNodeLogs(id,
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
            out(LogFormatter.formatBlock(client.getNodeLogs(id), format));
    }

}