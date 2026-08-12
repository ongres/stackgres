package io.stackgres.cli.commands.slon;

import io.stackgres.cli.Strings;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.Slon;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.UUID;

@Command(name = "get", description = "Get a PostgreSQL Slon (StackGres) by its ID")
public class GetSlonCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(description = "The Slon ID")
    UUID id;

    @Override
    public void run() {
        if (debug) client.setDebug();
        Slon slon = client.listSlons().stream()
                .filter(s -> s.id().equals(id))
                .findAny().orElse(null);

        if (slon == null)
            throw new IllegalArgumentException("No Slon found with ID: " + id);

        String output = """
                PostgreSQL Slon instance:

                ID:           $id$
                Port:         $port$
                Name:         $name$
                OS:           $os$
                Arch:         $arch$
                Version:      $version$
                CPUs:         $cpu$
                Memory:       $memory$"""
                .replace("$id$", slon.id().toString())
                .replace("$port$", String.valueOf(slon.port()))
                .replace("$name$", slon.name())
                .replace("$os$", slon.os())
                .replace("$arch$", slon.arch())
                .replace("$version$", slon.version())
                .replace("$cpu$", String.valueOf(slon.cpu()))
                .replace("$memory$", Strings.formatMemory(slon.memory()));
        outln(output);
    }

}