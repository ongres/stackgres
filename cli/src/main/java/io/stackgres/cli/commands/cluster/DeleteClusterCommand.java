package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.Strings;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.InteractivePrompt;
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

@Command(name = "delete", description = "Stops and deletes one or more PostgreSQL clusters", footer = "Either of @|yellow <name>|@, @|yellow --all|@, or @|yellow --tag|@ is required",
        customSynopsis = "@|bold stackgres cluster delete |@[@|yellow -fhX|@] (@|yellow <name>|@ | @|yellow --all|@)")
public class DeleteClusterCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Spec
    CommandSpec spec;

    @Parameters(description = "The cluster name", arity = "0..1")
    String name;

    @Option(names = {"-t", "--tag"}, description = "Only delete clusters that are tagged accordingly", split = ",", paramLabel = "<key=value>")
    Map<String, String> tags = new HashMap<>();

    @Option(names = {"-f", "--force"}, description = "Force deletion (doesn't ask for confirmation)")
    boolean force;

    @Option(names = {"-a", "--all"}, description = "Delete all clusters")
    boolean deleteAll;

    public DeleteClusterCommand() {
        super();
    }

    @Override
    public void run() {
        boolean tagsPresent = !tags.isEmpty();
        boolean namePresent = !Strings.isBlank(name);

        if ((deleteAll && tagsPresent && namePresent) || !(deleteAll ^ tagsPresent ^ namePresent))
            throw new CommandLine.MutuallyExclusiveArgsException(spec.commandLine(), "Specify exactly one of <name>, --all, or --tag");

        if (!force) {
            if (namePresent)
                outln("This will delete all data of the " + name + " cluster (including the PGDATA directory and PostgreSQL config)");
            else
                outln("This will delete all data of the clusters (including the PGDATA directories and PostgreSQL config)");
            if (!new InteractivePrompt(spec.commandLine()).confirm("delete"))
                throw new CommandLine.PicocliException("Aborted");
        }

        ProgressMessages messages = new ProgressMessages(spec.commandLine());
        if (debug) client.setDebug(messages);
        try {
            if (namePresent) {
                client.deleteCluster(name, string -> messages.doneAddFirstLine("The cluster " + string + " has been deleted"));
            } else if (deleteAll) {
                client.deleteAllClusters(string -> messages.add("- " + string + " deleted"));
                messages.doneAddFirstLine("All clusters have been deleted");
            } else if (tagsPresent) {
                client.deleteClusters(tags, string -> messages.add("- " + string + " deleted"));
                String tagString = tags.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
                messages.doneAddFirstLine("Clusters with tags (" + tagString + ") have been deleted");
            }
        } catch (Exception e) {
            throw new StackGresPicocliException(e, messages);
        }
    }

}