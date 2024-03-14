/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.resource.ResourceUtil.MEBIBYTES;

import java.math.BigDecimal;
import java.util.function.Function;

public enum StackGresInitContainer implements StackGresContainerProfile {

  SETUP_ARBITRARY_USER(StackGresGroupKind.CLUSTER, "setup-arbitrary-user",
      cpu -> cpu,
      memory -> memory),
  RELOCATE_BINARIES(StackGresGroupKind.CLUSTER, "relocate-binaries",
      cpu -> cpu,
      memory -> memory),
  PGBOUNCER_AUTH_FILE(StackGresGroupKind.CLUSTER, "pgbouncer-auth-file",
      cpu -> cpu,
      memory -> memory),
  DISTRIBUTEDLOGS_RECONCILIATION_CYCLE(StackGresGroupKind.CLUSTER,
      "distributedlogs-reconciliation-cycle",
      cpu -> cpu,
      memory -> memory),
  CLUSTER_RECONCILIATION_CYCLE(StackGresGroupKind.CLUSTER, "cluster-reconciliation-cycle",
      cpu -> cpu,
      memory -> memory),
  MAJOR_VERSION_UPGRADE(StackGresGroupKind.CLUSTER, "major-version-upgrade",
      cpu -> cpu,
      memory -> memory),
  RESET_PATRONI(StackGresGroupKind.CLUSTER, "reset-patroni",
      cpu -> cpu,
      memory -> memory),
  DBOPS_SET_DBOPS_RUNNING(StackGresGroupKind.DBOPS, "set-dbops-running",
      cpu -> BigDecimal.ONE,
      memory -> BigDecimal.valueOf(256).multiply(MEBIBYTES));

  public static final String CUSTOM = "custom-%s";

  private final StackGresGroupKind kind;
  private final String name;
  private final Function<BigDecimal, BigDecimal> cpuFormula;
  private final Function<BigDecimal, BigDecimal> memoryFormula;

  StackGresInitContainer(StackGresGroupKind kind, String name,
      Function<BigDecimal, BigDecimal> cpuFormula,
      Function<BigDecimal, BigDecimal> memoryFormula) {
    this.kind = kind;
    this.name = name;
    this.cpuFormula = cpuFormula;
    this.memoryFormula = memoryFormula;
  }

  @Override
  public StackGresGroupKind getKind() {
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

}
