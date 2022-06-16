/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public enum StackGresContainers {

  PATRONI("patroni"),
  ENVOY("envoy"),
  PGBOUNCER("pgbouncer"),
  POSTGRES_EXPORTER("prometheus-postgres-exporter"),
  POSTGRES_UTIL("postgres-util"),
  FLUENT_BIT("fluent-bit"),
  FLUENTD("fluentd"),
  CLUSTER_CONTROLLER("cluster-controller"),
  DISTRIBUTEDLOGS_CONTROLLER("distributedlogs-controller");

  private final String name;

  StackGresContainers(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return getName();
  }

}
