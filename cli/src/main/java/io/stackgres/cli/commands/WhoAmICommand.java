package io.stackgres.cli.commands;

import io.stackgres.cli.client.MatriarchClient;
import picocli.CommandLine.Command;

@Command(name = "whoami", description = "Displays the currently logged-in user")
public class WhoAmICommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Override
    public void run() {
        if (debug) client.setDebug();

        outln(client.getAccount());
    }

}