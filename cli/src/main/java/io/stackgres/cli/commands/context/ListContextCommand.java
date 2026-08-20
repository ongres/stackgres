package io.stackgres.cli.commands.context;

import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.config.CliConfig;
import io.stackgres.cli.config.Context;
import picocli.CommandLine.Command;

import java.util.List;

@Command(name = "list", description = "Lists the configured contexts", usageHelpWidth = 160)
public class ListContextCommand extends StackGresSubCommand {

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        List<Context> contexts = config.contexts();
        if (contexts.isEmpty()) {
            outln("No contexts configured. Add one with 'stackgres context set <name> --endpoint <host:port>'.");
            return;
        }
        String current = config.currentContext();
        outf("%-3s%-18s%-30s%-7s%-16s\n", "", "NAME", "ENDPOINT", "TLS", "ENVIRONMENT");
        for (Context c : contexts) {
            String marker = c.name().equals(current) ? "*" : "";
            String tls = c.tls() == null ? "auto" : c.tls().toString();
            outf("%-3s%-18s%-30s%-7s%-16s\n", marker, c.name(), nvl(c.endpoint()), tls, nvl(c.environment()));
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
