/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgutils;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;

@Sidecar(StackgresClusterContainers.POSTGRES_UTIL)
@Singleton
public class PostgresUtil implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String NAME = StackgresClusterContainers.POSTGRES_UTIL;

  public PostgresUtil() {
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
            ClusterStatefulSetVolumeConfig.USER.volumeMount(context))
        .build();
  }

}
