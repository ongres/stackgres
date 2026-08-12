package io.stackgres.cli.postgres;

import java.util.UUID;

public record Slon(UUID id,
                   int port,
                   String name,
                   String os,
                   String arch,
                   String version,
                   double cpu,
                   long memory) {
}