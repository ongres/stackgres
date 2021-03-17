/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgutils;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetPath;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;

@Sidecar(StackgresClusterContainers.POSTGRES_UTIL)
@Singleton
public class PostgresUtil implements StackGresClusterSidecarResourceFactory<Void> {

  private static final String NAME = StackgresClusterContainers.POSTGRES_UTIL;
  public static final String IMAGE_NAME = "docker.io/ongres/postgres-util:v%s-build-%s";

  public PostgresUtil() {
  }

  @Override
  public Container getContainer(StackGresGeneratorContext context) {
    String pgVersion = context.getClusterContext().getCluster().getSpec().getPostgresVersion();

    return new ContainerBuilder()
        .withName(NAME)
        .withImage(String.format(IMAGE_NAME, pgVersion,
            StackGresProperty.CONTAINER_BUILD.getString()))
        .withImagePullPolicy("IfNotPresent")
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh")
        .withArgs("-c", "while true; do sleep 10; done")
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.SOCKET.volumeMount(context.getClusterContext()),
            ClusterStatefulSetVolumeConfig.LOCAL.volumeMount(
                ClusterStatefulSetPath.ETC_PASSWD_PATH, context.getClusterContext()),
            ClusterStatefulSetVolumeConfig.LOCAL.volumeMount(
                ClusterStatefulSetPath.ETC_GROUP_PATH, context.getClusterContext()),
            ClusterStatefulSetVolumeConfig.LOCAL.volumeMount(
                ClusterStatefulSetPath.ETC_SHADOW_PATH, context.getClusterContext()),
            ClusterStatefulSetVolumeConfig.LOCAL.volumeMount(
                ClusterStatefulSetPath.ETC_GSHADOW_PATH, context.getClusterContext()))
        .build();
  }

}
