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

  PATRONI(StackGresGroupKind.CLUSTER, "patroni",
      cpu -> cpu,
      memory -> memory),
  ENVOY(StackGresGroupKind.CLUSTER, "envoy",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(4)))),
      memory -> BigDecimal.valueOf(64).multiply(MEBIBYTES)
      ),
  PGBOUNCER(StackGresGroupKind.CLUSTER, "pgbouncer",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
      memory -> BigDecimal.valueOf(64).multiply(MEBIBYTES)
      ),
  POSTGRES_EXPORTER(StackGresGroupKind.CLUSTER, "prometheus-postgres-exporter",
      cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16))),
      memory -> BigDecimal.valueOf(256).multiply(MEBIBYTES)
      ),
  POSTGRES_UTIL(StackGresGroupKind.CLUSTER, "postgres-util",
      cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16))),
      memory -> BigDecimal.valueOf(64).multiply(MEBIBYTES)
      ),
  FLUENT_BIT(StackGresGroupKind.CLUSTER, "fluent-bit",
      cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16))),
      memory -> BigDecimal.valueOf(64).multiply(MEBIBYTES)
      ),
  FLUENTD(StackGresGroupKind.CLUSTER, "fluentd",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(4)))),
      memory -> BigDecimal.valueOf(2).multiply(GIBIBYTES)
      ),
  CLUSTER_CONTROLLER(StackGresGroupKind.CLUSTER, "cluster-controller",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
      memory -> BigDecimal.valueOf(512).multiply(MEBIBYTES)
      ),
  DISTRIBUTEDLOGS_CONTROLLER(StackGresGroupKind.CLUSTER, "distributedlogs-controller",
      cpu -> BigDecimal.ONE.divide(BigDecimal.valueOf(4))
          .max(BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
      memory -> BigDecimal.valueOf(512).multiply(MEBIBYTES)
      ),
  DBOPS_RUN_DBOPS(StackGresGroupKind.DBOPS, "run-dbops",
      cpu -> BigDecimal.ONE,
      memory -> BigDecimal.valueOf(256).multiply(MEBIBYTES)
      ),
  DBOPS_SET_DBOPS_RESULT(StackGresGroupKind.DBOPS, "set-dbops-result",
      cpu -> BigDecimal.ONE,
      memory -> BigDecimal.valueOf(256).multiply(MEBIBYTES)
      ),
  BACKUP_CREATE_BACKUP(StackGresGroupKind.BACKUP, "create-backup",
      cpu -> BigDecimal.ONE,
      memory -> BigDecimal.valueOf(256).multiply(MEBIBYTES)
      ),
  STREAM_CONTROLLER(StackGresGroupKind.STREAM, "stream-controller",
      cpu -> BigDecimal.ONE,
      memory -> BigDecimal.valueOf(512).multiply(MEBIBYTES)
      );

  public static final String CUSTOM = "custom-%s";

  private final StackGresGroupKind kind;
  private final String name;
  private final Function<BigDecimal, BigDecimal> cpuFormula;
  private final Function<BigDecimal, BigDecimal> memoryFormula;

  StackGresContainer(StackGresGroupKind kind, String name,
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
