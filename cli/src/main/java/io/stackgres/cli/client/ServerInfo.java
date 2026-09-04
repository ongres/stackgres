package io.stackgres.cli.client;

/**
 * The identity of the endpoint the CLI is directly connected to, from api.v1 {@code GetServerInfo}:
 * its build {@code version}, the {@code component} it is ("matriarch" for a local/standalone matriarch,
 * "cloud" for the cloud edge), and an optional short git {@code commit}. Powers the Server block of
 * {@code stackgres version}.
 */
public record ServerInfo(String version, String component, String commit) {
}
