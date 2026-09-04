package io.stackgres.cli.commands;

import io.stackgres.cli.CliContext;
import io.stackgres.cli.VersionInfo;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.client.ServerInfo;
import io.stackgres.cli.config.ResolvedContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prints the CLI's own build stamp (Client) and, unless {@code --client}, the version of the endpoint it
 * points at (Server) — a local matriarch or the cloud, whichever it's connected to. Mirrors the
 * docker/kubectl Client+Server split and shares its client stamp with {@code --version}. Human output
 * uses the shared labeled-field style; {@code -o json|yaml} is for scripting.
 */
@Command(name = "version", description = "Shows the client (and server) version")
public class VersionCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Option(names = "--client", description = "Show only the client version (no server call)")
    boolean clientOnly;

    @Option(names = {"-o", "--output"}, paramLabel = "<format>",
            description = "Output format: text (default), json, yaml")
    String output;

    @Override
    public void run() {
        if (debug) client.setDebug();
        VersionInfo v = VersionInfo.INSTANCE;
        ResolvedContext ctx = CliContext.resolve();

        // Server block is best-effort: an old/unreachable endpoint (or one that predates GetServerInfo)
        // must not fail `version` — we still print the client stamp.
        ServerInfo server = null;
        String serverError = null;
        if (!clientOnly) {
            try {
                server = client.getServerInfo();
            } catch (RuntimeException e) {
                serverError = e.getMessage();
            }
        }

        String fmt = output == null || output.isBlank() ? "text" : output.toLowerCase();
        switch (fmt) {
            case "text" -> renderText(v, ctx, server, serverError);
            case "json" -> outln(renderJson(dataMap(v, ctx, server)));
            case "yaml" -> outln(renderYaml(dataMap(v, ctx, server)));
            default -> throw new IllegalArgumentException(
                    "unknown output format '" + output + "' (use text, json, or yaml)");
        }
    }

    private void renderText(VersionInfo v, ResolvedContext ctx, ServerInfo server, String serverError) {
        outln("Client:");
        field("Version", v.version());
        if (v.commit() != null) {
            field("Commit", v.commit());
        }
        if (v.built() != null) {
            field("Built", v.built());
        }
        field("Runtime", v.runtime());
        field("Platform", v.platform());
        if (clientOnly) {
            return;
        }
        outln("");
        outln("Server:");
        field("Endpoint", ctx.endpoint() + (ctx.tls() ? "" : " (plaintext)"));
        if (server != null) {
            field("Component", server.component());
            field("Version", server.version());
            if (server.commit() != null && !server.commit().isBlank()) {
                field("Commit", server.commit());
            }
        } else {
            field("Status", "unavailable" + (serverError != null ? " (" + serverError + ")" : ""));
        }
    }

    /** Ordered lowercase-keyed sections for machine output (client, [server]). */
    private Map<String, Map<String, String>> dataMap(VersionInfo v, ResolvedContext ctx, ServerInfo server) {
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        Map<String, String> c = new LinkedHashMap<>();
        c.put("version", v.version());
        if (v.commit() != null) {
            c.put("commit", v.commit());
        }
        if (v.built() != null) {
            c.put("built", v.built());
        }
        c.put("runtime", v.runtime());
        c.put("platform", v.platform());
        data.put("client", c);
        if (!clientOnly) {
            Map<String, String> s = new LinkedHashMap<>();
            s.put("endpoint", ctx.endpoint());
            if (server != null) {
                s.put("component", server.component());
                s.put("version", server.version());
                if (server.commit() != null && !server.commit().isBlank()) {
                    s.put("commit", server.commit());
                }
            } else {
                s.put("status", "unavailable");
            }
            data.put("server", s);
        }
        return data;
    }

    private static String renderJson(Map<String, Map<String, String>> data) {
        StringBuilder sb = new StringBuilder("{");
        boolean firstSection = true;
        for (Map.Entry<String, Map<String, String>> section : data.entrySet()) {
            if (!firstSection) {
                sb.append(",");
            }
            firstSection = false;
            sb.append(jsonStr(section.getKey())).append(":{");
            boolean first = true;
            for (Map.Entry<String, String> e : section.getValue().entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(jsonStr(e.getKey())).append(":").append(jsonStr(e.getValue()));
            }
            sb.append("}");
        }
        return sb.append("}").toString();
    }

    private static String jsonStr(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String renderYaml(Map<String, Map<String, String>> data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> section : data.entrySet()) {
            sb.append(section.getKey()).append(":\n");
            for (Map.Entry<String, String> e : section.getValue().entrySet()) {
                sb.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
        }
        return sb.toString().stripTrailing();
    }
}
