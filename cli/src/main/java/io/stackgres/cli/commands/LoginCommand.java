package io.stackgres.cli.commands;

import io.stackgres.cli.Jwt;
import io.stackgres.cli.client.TokenExchange;
import io.stackgres.cli.config.CliConfig;
import io.stackgres.cli.config.Context;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Authenticates the CLI against the cloud: exchanges a one-time token (OTT, minted in the web console)
 * for a JWT, saves it in a context, and makes that context current — the CLI equivalent of the "connect
 * CLI" command shown in the UI. The endpoint defaults to {@link #DEFAULT_ENDPOINT} (HTTPS, default port);
 * override with {@code --endpoint}. The token is prompted (hidden) when not given as an argument, so it
 * stays out of shell history.
 */
@Command(name = "login", description = "Logs in to the cloud by exchanging a one-time token for a saved session")
public class LoginCommand extends StackGresSubCommand {

    /** The default cloud endpoint (HTTPS, default port). Override with --endpoint. */
    public static final String DEFAULT_ENDPOINT = "dev-cc.stackgres.best";

    @Parameters(index = "0", arity = "0..1", paramLabel = "<one-time-token>",
            description = "The one-time token from the web console (prompted if omitted)")
    String oneTimeToken;

    @Option(names = "--endpoint", paramLabel = "<host[:port]>",
            description = "The cloud endpoint (default: " + DEFAULT_ENDPOINT + ")")
    String endpoint;

    @Option(names = "--name", paramLabel = "<name>",
            description = "Name for the saved context (default: the endpoint host)")
    String contextName;

    @Override
    public void run() {
        String ep = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim();
        String ott = oneTimeToken == null || oneTimeToken.isBlank() ? promptToken() : oneTimeToken.trim();
        if (ott.isBlank()) {
            throw new RuntimeException("no one-time token provided");
        }
        String jwt = TokenExchange.exchange(ep, ott);

        String name = contextName == null || contextName.isBlank() ? contextNameFor(ep) : contextName.trim();
        CliConfig config = CliConfig.load();
        // Preserve a prior environment selection on re-login into the same context.
        String environment = config.find(name).map(Context::environment).orElse(null);
        config.upsert(new Context(name, ep, true, jwt, environment));
        config.use(name);
        config.save();

        String user = Jwt.subject(jwt);
        outln("Logged in" + (user != null ? " as " + user : "") + " at " + ep + " (context '" + name + "').");
    }

    private String promptToken() {
        Console console = System.console();
        if (console != null) {
            char[] input = console.readPassword("One-time token: ");
            return input == null ? "" : new String(input).trim();
        }
        // No interactive console (piped) — read a line from stdin.
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String line = reader.readLine();
            return line == null ? "" : line.trim();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * A short context name from an endpoint: its first DNS label ({@code dev-cc.stackgres.best} →
     * {@code dev-cc}). Keeps the whole host when there is no dot ({@code localhost}) or when it looks
     * like an IP ({@code 10.0.0.5}, where a first label of {@code 10} would be useless).
     */
    private static String contextNameFor(String endpoint) {
        String host = hostOf(endpoint);
        int dot = host.indexOf('.');
        if (dot <= 0 || Character.isDigit(host.charAt(0))) {
            return host;
        }
        return host.substring(0, dot);
    }

    private static String hostOf(String endpoint) {
        int colon = endpoint.indexOf(':');
        return colon > 0 ? endpoint.substring(0, colon) : endpoint;
    }
}
