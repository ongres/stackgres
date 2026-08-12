package io.stackgres.cli.commands.slon;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.Slon;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(name = "list", description = "Lists the PostgreSQL Slon (StackGres) resources")
public class ListSlonCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Option(names = {"-q", "--quiet"}, description = "Only display the Slon ID")
    boolean quiet;

    @Override
    public void run() {
        if (debug) client.setDebug();
        List<Slon> slons = client.listSlons();

        if (quiet) {
            slons.stream().map(slon -> slon.id().toString()).forEach(this::outln);
            return;
        }

        if (slons.isEmpty()) {
            outln("There are no PostgreSQL Slons");
            return;
        }

        int nameMaxLength = slons.stream().map(Slon::name).mapToInt(String::length).max().orElseThrow();
        int nameLength = (nameMaxLength <= 8) ? 10 : (nameMaxLength + 2);
        int osMaxLength = slons.stream().map(Slon::os).mapToInt(String::length).max().orElseThrow();
        int osLength = (osMaxLength <= 8) ? 10 : (osMaxLength + 2);

        outf("%-38s%-" + nameLength + "s%-" + osLength + "s%-10s%-10s\n", "ID", "Name", "OS", "CPUs", "Port");
        slons.forEach(c -> outf("%-38s%-" + nameLength + "s%-" + osLength + "s%-10s%-10s\n", c.id(), c.name(), c.os(), c.cpu(), c.port()));
    }

}