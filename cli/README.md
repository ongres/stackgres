# StackGres Command Line

This is the StackGres command line (`stackgres`), which manages your StackGres clusters.

## Usage

The following shows how to get started quickly:

```
$> stackgres cluster list
There are no PostgreSQL clusters running

$> stackgres cluster create
✓ Creating PostgreSQL cluster
Cluster default created
Cluster default started successfully

List all StackGres clusters with the following command: stackgres cluster list

$> stackgres cluster list
Name      Status    Version   Port
default   Healthy   16.3      5432
```

## Building

Run `mvn clean package` to build the JVM executable fat JAR.

Run `mvn clean package -Pnative` to build the native executable.
This requires a GraalVM installation (the `java` binary or the `GRAALVM_HOME` env var being set).

## Configuration Options

The CLI resolves its connection target with the precedence **flag > environment variable > current
context (`~/.stackgres/config.yaml`) > default**. The environment variables:

| Environment variable      | Description                                                                                          |
|:--------------------------|:-----------------------------------------------------------------------------------------------------|
| `STACKGRES_ENDPOINT_URL`  | The gRPC endpoint (matriarch or cloud) the CLI connects to, `host:port` (default: `localhost:50051`). |
| `STACKGRES_ENDPOINT_TLS`  | Force TLS on/off (`true`/`false`). Default: auto — TLS unless the endpoint is `localhost:`.           |
| `STACKGRES_TOKEN`         | Bearer JWT for authentication (required by the cloud; a local matriarch is unauthenticated).          |
| `STACKGRES_ENVIRONMENT`   | The target environment id for api.v1 calls (default: the context's environment, or `local`).         |
| `STACKGRES_CONTEXT`       | The named context to use, overriding the config's current-context.                                   |
| `STACKGRES_CONFIG`        | Path to the config file (default: `~/.stackgres/config.yaml`).                                        |

The same `STACKGRES_ENDPOINT_URL` / `STACKGRES_TOKEN` names are used by the matriarch (k8s operator and
standalone) for its uplink to the cloud, so "the endpoint I connect to" is one consistent concept
everywhere. Prefer named contexts (`stackgres context set …`) over env vars for day-to-day use.
