package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresPicocliException;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.commands.ProgressMessages;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "start", description = "Starts one or more (previously stopped) PostgreSQL clusters", footer = "Either of @|yellow <name...>|@, @|yellow --all|@, or @|yellow --tag|@ is required",
        customSynopsis = "@|bold stackgres cluster start |@[@|yellow -hX|@] (@|yellow <name...>|@ | @|yellow --all|@ | @|yellow --tag|@=@|italic <key=value>|@)")
public class StartClusterCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Spec
    CommandSpec spec;

    @Parameters(description = "One or more cluster names", arity = "0..*", paramLabel = "<name>")
    List<String> names;

    @Option(names = {"-t", "--tag"}, description = "Only start clusters that are tagged accordingly", split = ",", paramLabel = "<key=value>")
    Map<String, String> tags = new HashMap<>();

    @Option(names = {"-a", "--all"}, description = "Start all stopped clusters")
    boolean startAll;

    public StartClusterCommand() {
        super();
    }

    @Override
    public void run() {
        boolean tagsPresent = !tags.isEmpty();
        boolean namesPresent = names != null && !names.isEmpty();

        if ((startAll ? 1 : 0) + (tagsPresent ? 1 : 0) + (namesPresent ? 1 : 0) != 1)
            throw new CommandLine.MutuallyExclusiveArgsException(spec.commandLine(), "Specify exactly one of <name...>, --all, or --tag");

        ProgressMessages messages = new ProgressMessages(spec.commandLine());
        if (debug) client.setDebug(messages);
        try {
            if (namesPresent) {
                runBatch(names, messages, "cluster", "started", client::startCluster);
            } else if (startAll) {
                client.startAllClusters();
                messages.doneAddFirstLine("All clusters have been started");
            } else {
                client.startClusters(tags);
                String tagString = tags.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
                messages.doneAddFirstLine("Clusters with tags (" + tagString + ") have been started");
            }
        } catch (StackGresPicocliException e) {
            throw e;
        } catch (Exception e) {
            throw new StackGresPicocliException(e, messages);
        }
    }

}
