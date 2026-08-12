package io.stackgres.cli.postgres;

import java.util.List;

public record ClusterDiagnostics(List<InstanceDiagnostics> instances) {
}
