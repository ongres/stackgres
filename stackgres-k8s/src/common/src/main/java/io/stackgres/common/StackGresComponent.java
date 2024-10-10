/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;

import io.stackgres.common.component.Component;
import io.stackgres.common.component.Components;
import io.stackgres.common.component.DocirComponents;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;

public enum StackGresComponent {

  POSTGRESQL("postgresql"),
  BABELFISH("babelfish"),
  WALG("walg"),
  HDRHISTOGRAM("hdrhistogram"),
  PATRONI("patroni"),
  POSTGRES_UTIL("postgresql"),
  PGBOUNCER("pgbouncer"),
  PROMETHEUS_POSTGRES_EXPORTER("prometheus-postgres-exporter"),
  ENVOY("envoy"),
  FLUENT_BIT("fluentbit"),
  FLUENTD("fluentd"),
  KUBECTL("kubectl"),
  BABELFISH_COMPASS("babelfish-compass"),
  OTEL_COLLECTOR("otel-collector");

  public static final String LATEST = "latest";

  final String name;

  StackGresComponent(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Component getOldest() {
    return getOrThrow(StackGresVersion.OLDEST);
  }

  public Component getLatest() {
    return getOrThrow(StackGresVersion.LATEST);
  }

  public boolean has(StackGresCluster cluster) {
    return get(StackGresVersion.getStackGresVersion(cluster)).isPresent()
        || (StackGresUtil.isRegistryEnabled(cluster) && getDocir(cluster).isPresent());
  }

  /**
   * The component for the cluster: the one served by the docir repository of the cluster when
   * {@code spec.configurations.registry.enabled} is {@code true} and the component images are
   * combined by docir, the one bundled with the operator version of the cluster otherwise.
   */
  public Component get(StackGresCluster cluster) {
    if (StackGresUtil.isRegistryEnabled(cluster)) {
      return getDocir(cluster)
          .orElseGet(() -> getOrThrow(StackGresVersion.getStackGresVersion(cluster)));
    }
    return getOrThrow(StackGresVersion.getStackGresVersion(cluster));
  }

  public Component get(StackGresShardedCluster cluster) {
    if (StackGresUtil.isRegistryEnabled(cluster)) {
      return getDocir(cluster)
          .orElseGet(() -> getOrThrow(StackGresVersion.getStackGresVersion(cluster)));
    }
    return getOrThrow(StackGresVersion.getStackGresVersion(cluster));
  }

  public Component get(StackGresDistributedLogs distributedLogs) {
    return getOrThrow(StackGresVersion.getStackGresVersion(distributedLogs));
  }

  public Optional<Component> get(StackGresVersion version) {
    return Optional.of(Components.getComponentVersionMap(this))
        .map(map -> map.get(version));
  }

  public Component getOrThrow(StackGresVersion version) {
    return get(version)
        .orElseThrow(() -> new IllegalArgumentException(
            "StackGres version " + version + " not supported"));
  }

  private Optional<Component> getDocir(StackGresCluster cluster) {
    return DocirComponents.of(StackGresUtil.getRegistryUri(cluster)).getComponent(this);
  }

  private Optional<Component> getDocir(StackGresShardedCluster cluster) {
    return DocirComponents.of(StackGresUtil.getRegistryUri(cluster)).getComponent(this);
  }

}
