package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.Strings;
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
import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "start", description = "Starts one or more (previously stopped) PostgreSQL clusters", footer = "Either of @|yellow <name>|@, @|yellow --all|@, or @|yellow --tag|@ is required",
        customSynopsis = "@|bold stackgres cluster start |@[@|yellow -hX|@] (@|yellow <name>|@ | @|yellow --all|@ | @|yellow --tag|@=@|italic <key=value>|@)")
public class StartClusterCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Spec
    CommandSpec spec;

    @Parameters(description = "The cluster name", arity = "0..1")
    String name;

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
        boolean namePresent = !Strings.isBlank(name);

        if ((startAll && tagsPresent && namePresent) || !(startAll ^ tagsPresent ^ namePresent))
            throw new CommandLine.MutuallyExclusiveArgsException(spec.commandLine(), "Specify exactly one of <name>, --all, or --tag");

        ProgressMessages messages = new ProgressMessages(spec.commandLine());
        if (debug) client.setDebug(messages);
        try {
            if (namePresent) {
                client.startCluster(name);
                messages.doneAddFirstLine("The cluster " + name + " has been started");
            } else if (startAll) {
                client.startAllClusters();
                messages.doneAddFirstLine("All clusters have been started");
            } else if (tagsPresent) {
                client.startClusters(tags);
                String tagString = tags.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
                messages.doneAddFirstLine("Clusters with tags (" + tagString + ") have been started");
            }
        } catch (Exception e) {
            throw new StackGresPicocliException(e, messages);
        }
    }

}
