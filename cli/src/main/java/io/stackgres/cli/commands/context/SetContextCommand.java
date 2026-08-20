package io.stackgres.cli.commands.context;

import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.config.CliConfig;
import io.stackgres.cli.config.Context;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "set", description = "Adds or updates a context")
public class SetContextCommand extends StackGresSubCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "The context name")
    String name;

    @Option(names = "--endpoint", paramLabel = "<host:port>", description = "The matriarch/cloud gRPC endpoint")
    String endpoint;

    @Option(names = "--token", paramLabel = "<jwt>", description = "Bearer token for authentication")
    String token;

    @Option(names = "--tls", arity = "1", paramLabel = "<true|false>", description = "Force TLS on/off (default: auto — TLS unless localhost)")
    Boolean tls;

    // Named --default-env, not --environment, to avoid clashing with the inherited global -E/--environment.
    @Option(names = "--default-env", paramLabel = "<id>", description = "The environment this context targets by default")
    String environment;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        Context existing = config.find(name).orElse(null);
        Context merged = new Context(name,
                endpoint != null ? endpoint : (existing == null ? null : existing.endpoint()),
                tls != null ? tls : (existing == null ? null : existing.tls()),
                token != null ? token : (existing == null ? null : existing.token()),
                environment != null ? environment : (existing == null ? null : existing.environment()));
        config.upsert(merged);
        if (config.currentContext() == null) {
            config.use(name);   // first context becomes current for convenience
        }
        config.save();
        outln((existing == null ? "Created" : "Updated") + " context '" + name + "'.");
    }
}
