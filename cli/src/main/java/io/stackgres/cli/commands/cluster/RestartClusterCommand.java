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

@Command(name = "restart", description = "Stops and restarts one or more PostgreSQL clusters", footer = "Either of @|yellow <name>|@, @|yellow --all|@, or @|yellow --tag|@ is required",
        customSynopsis = "@|bold stackgres cluster restart |@[@|yellow -hX|@] (@|yellow <name>|@ | @|yellow --all|@ | @|yellow --tag|@=@|italic <key=value>|@)")
public class RestartClusterCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Spec
    CommandSpec spec;

    @Parameters(description = "The cluster name", arity = "0..1")
    String name;

    @Option(names = {"-t", "--tag"}, description = "Only restart clusters that are tagged accordingly", split = ",", paramLabel = "<key=value>")
    Map<String, String> tags = new HashMap<>();

    @Option(names = {"-a", "--all"}, description = "Restart all clusters")
    boolean restartAll;

    public RestartClusterCommand() {
        super();
    }

    @Override
    public void run() {
        boolean tagsPresent = !tags.isEmpty();
        boolean namePresent = !Strings.isBlank(name);

        if ((restartAll && tagsPresent && namePresent) || !(restartAll ^ tagsPresent ^ namePresent))
            throw new CommandLine.MutuallyExclusiveArgsException(spec.commandLine(), "Specify exactly one of <name>, --all, or --tag");

        // Resolve (and note the target environment) before the spinner starts, so any cross-environment
        // note or ambiguity error renders cleanly instead of colliding with ProgressMessages.
        if (namePresent) client.resolveCluster(name, "Targeting");

        ProgressMessages messages = new ProgressMessages(spec.commandLine());
        if (debug) client.setDebug(messages);
        try {
            if (namePresent) {
                client.restartCluster(name);
                messages.doneAddFirstLine("The cluster " + name + " has been restarted");
            } else if (restartAll) {
                client.restartAllClusters();
                messages.doneAddFirstLine("All clusters have been restarted");
            } else if (tagsPresent) {
                client.restartClusters(tags);
                String tagString = tags.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
                messages.doneAddFirstLine("Clusters with tags (" + tagString + ") have been restarted");
            }
        } catch (Exception e) {
            throw new StackGresPicocliException(e, messages);
        }
    }

}
