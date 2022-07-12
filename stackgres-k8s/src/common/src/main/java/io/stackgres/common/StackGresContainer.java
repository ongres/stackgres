/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.resource.ResourceUtil.GIBIBYTES;
import static io.stackgres.common.resource.ResourceUtil.MEBIBYTES;

import java.math.BigDecimal;
import java.util.function.Function;

public enum StackGresContainer implements StackGresContainerProfile {

  PATRONI(StackGresKind.CLUSTER, "patroni",
      cpu -> cpu,
      memory -> memory),
  ENVOY(StackGresKind.CLUSTER, "envoy",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(4)))),
      memory -> BigDecimal.valueOf(64).multiply(MEBIBYTES)
      ),
  PGBOUNCER(StackGresKind.CLUSTER, "pgbouncer",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
      memory -> BigDecimal.valueOf(64).multiply(MEBIBYTES)
      ),
  POSTGRES_EXPORTER(StackGresKind.CLUSTER, "prometheus-postgres-exporter",
      cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16))),
      memory -> BigDecimal.valueOf(8).multiply(MEBIBYTES)
      ),
  POSTGRES_UTIL(StackGresKind.CLUSTER, "postgres-util",
      cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16))),
      memory -> BigDecimal.valueOf(8).multiply(MEBIBYTES)
      ),
  FLUENT_BIT(StackGresKind.CLUSTER, "fluent-bit",
      cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16))),
      memory -> BigDecimal.valueOf(8).multiply(MEBIBYTES)
      ),
  FLUENTD(StackGresKind.CLUSTER, "fluentd",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(4)))),
      memory -> BigDecimal.valueOf(2).multiply(GIBIBYTES)
      ),
  CLUSTER_CONTROLLER(StackGresKind.CLUSTER, "cluster-controller",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
      memory -> BigDecimal.valueOf(512).multiply(MEBIBYTES)
      ),
  DISTRIBUTEDLOGS_CONTROLLER(StackGresKind.CLUSTER, "distributedlogs-controller",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
      memory -> BigDecimal.valueOf(512).multiply(MEBIBYTES)
      ),
  DBOPS_RUN_DBOPS(StackGresKind.DBOPS, "run-dbops",
      cpu -> BigDecimal.ONE,
      memory -> BigDecimal.valueOf(64).multiply(MEBIBYTES)
      ),
  DBOPS_SET_DBOPS_RESULT(StackGresKind.DBOPS, "set-dbops-result",
      cpu -> BigDecimal.ONE,
      memory -> BigDecimal.valueOf(64).multiply(MEBIBYTES)
      ),
  BACKUP_CREATE_BACKUP(StackGresKind.BACKUP, "create-backup",
      cpu -> BigDecimal.ONE,
      memory -> BigDecimal.valueOf(64).multiply(MEBIBYTES)
      );

  private final StackGresKind kind;
  private final String name;
  private final Function<BigDecimal, BigDecimal> cpuFormula;
  private final Function<BigDecimal, BigDecimal> memoryFormula;

  StackGresContainer(StackGresKind kind, String name,
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

}
