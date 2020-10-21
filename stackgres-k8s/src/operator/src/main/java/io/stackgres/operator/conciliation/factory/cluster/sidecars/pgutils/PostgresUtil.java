/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgutils;

import java.util.List;

import java.util.Map;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;

@Sidecar(StackgresClusterContainers.POSTGRES_UTIL)
@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
@RunningContainer(order = 5)
public class PostgresUtil implements ContainerFactory<StackGresClusterContext> {

  public static final String NAME = StackgresClusterContainers.POSTGRES_UTIL;

  public PostgresUtil() {
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContext context) {
    return ImmutableMap.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        StackGresComponent.POSTGRESQL.findVersion(
            context.getCluster().getSpec().getPostgresVersion()));
  }

  @Override
  public Container getContainer(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName(NAME)
        .withImage(StackGresComponent.POSTGRES_UTIL.findImageName(
            context.getCluster().getSpec().getPostgresVersion()))
        .withImagePullPolicy("IfNotPresent")
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh")
        .withArgs("-c", "while true; do sleep 10; done")
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.SOCKET.volumeMount(context),
            ClusterStatefulSetVolumeConfig.EMPTY_BASE.volumeMount(context))
        .addAllToVolumeMounts(ClusterStatefulSetVolumeConfig.USER.volumeMounts(context))
        .build();
  }

  @Override
  public List<Volume> getVolumes(StackGresClusterContext context) {
    return List.of();
  }

}
