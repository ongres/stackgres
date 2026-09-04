package io.stackgres.cli.commands.context;

import io.stackgres.cli.Jwt;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.config.CliConfig;
import io.stackgres.cli.config.Context;
import picocli.CommandLine.Command;

import java.util.ArrayList;
import java.util.List;

@Command(name = "list", description = "Lists the configured contexts")
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

        // A context authenticates with a bearer JWT or nothing (e.g. a local matriarch); flag a JWT past
        // its exp. The AUTH column auto-sizes since "JWT (expired)" is much wider than "none".
        List<String> auths = new ArrayList<>(contexts.size());
        int authLen = "Auth".length();
        for (Context c : contexts) {
            String auth = c.token() == null || c.token().isBlank()
                    ? "none" : (Jwt.isExpired(c.token()) ? "JWT (expired)" : "JWT");
            auths.add(auth);
            authLen = Math.max(authLen, auth.length());
        }
        String fmt = "%-3s%-18s%-30s%-7s%-" + (authLen + 2) + "s%-16s\n";

        outf(fmt, "", "Name", "Endpoint", "TLS", "Auth", "Environment");
        for (int i = 0; i < contexts.size(); i++) {
            Context c = contexts.get(i);
            String marker = c.name().equals(current) ? "*" : "";
            String tls = c.tls() == null ? "auto" : c.tls().toString();
            outf(fmt, marker, c.name(), nvl(c.endpoint()), tls, auths.get(i), nvl(c.environment()));
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
