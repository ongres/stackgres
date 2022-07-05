/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.resource.ResourceUtil.MEBIBYTES;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Function;

import io.fabric8.kubernetes.client.CustomResource;

public enum StackGresInitContainer implements StackGresContainerProfile {

  SETUP_ARBITRARY_USER(StackGresKind.CLUSTER, "setup-arbitrary-user",
      cpu -> cpu,
      memory -> memory),
  SETUP_DATA_PATHS(StackGresKind.CLUSTER, "setup-data-paths",
      cpu -> cpu,
      memory -> memory),
  SETUP_SCRIPTS(StackGresKind.CLUSTER, "setup-scripts",
      cpu -> cpu,
      memory -> memory),
  RELOCATE_BINARIES(StackGresKind.CLUSTER, "relocate-binaries",
      cpu -> cpu,
      memory -> memory),
  PGBOUNCER_AUTH_FILE(StackGresKind.CLUSTER, "pgbouncer-auth-file",
      cpu -> cpu,
      memory -> memory),
  DISTRIBUTEDLOGS_RECONCILIATION_CYCLE(StackGresKind.CLUSTER,
      "distributedlogs-reconciliation-cycle",
      cpu -> cpu,
      memory -> memory),
  CLUSTER_RECONCILIATION_CYCLE(StackGresKind.CLUSTER, "cluster-reconciliation-cycle",
      cpu -> cpu,
      memory -> memory),
  MAJOR_VERSION_UPGRADE(StackGresKind.CLUSTER, "major-version-upgrade",
      cpu -> cpu,
      memory -> memory),
  RESET_PATRONI(StackGresKind.CLUSTER, "reset-patroni",
      cpu -> cpu,
      memory -> memory),
  DBOPS_SET_DBOPS_RUNNING(StackGresKind.DBOPS, "set-dbops-running",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
      memory -> BigDecimal.valueOf(8).multiply(MEBIBYTES));

  private final StackGresKind kind;
  private final String name;
  private final Function<BigDecimal, BigDecimal> cpuFormula;
  private final Function<BigDecimal, BigDecimal> memoryFormula;

  StackGresInitContainer(StackGresKind kind, String name,
      Function<BigDecimal, BigDecimal> cpuFormula,
      Function<BigDecimal, BigDecimal> memoryFormula) {
    this.kind = kind;
    this.name = name;
    this.cpuFormula = cpuFormula;
    this.memoryFormula = memoryFormula;
  }

  @Override
  public StackGresKind getKind() {
    return kind;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Function<BigDecimal, BigDecimal> getCpuFormula() {
    return cpuFormula;
  }

  @Override
  public Function<BigDecimal, BigDecimal> getMemoryFormula() {
    return memoryFormula;
  }

  @Override
  public String toString() {
    return getName();
  }

  public static StackGresInitContainer getFromNameWithPrefixFor(
      Class<? extends CustomResource<?, ?>> kind, String nameWithPrefix) {
    for (StackGresInitContainer container : values()) {
      if (Objects.equals(container.getNameWithPrefix(), nameWithPrefix)) {
        return container;
      }
    }
    return null;
  }

}
