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

| <div style="width:20rem">Option</div> | Description                                                                                       |
|:--------------------------------------|---------------------------------------------------------------------------------------------------|
| `STACKGRES_MATRIARCH_URL`             | The base URL of the Matriarch server that the CLI communicates with (default: `localhost:50051`). |
