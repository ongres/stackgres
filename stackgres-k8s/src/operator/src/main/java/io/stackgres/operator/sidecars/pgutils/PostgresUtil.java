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
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;

@Sidecar(StackgresClusterContainers.POSTGRES_UTIL)
@Singleton
public class PostgresUtil implements StackGresClusterSidecarResourceFactory<Void> {

  private static final String NAME = StackgresClusterContainers.POSTGRES_UTIL;
  private static final String IMAGE_NAME = "docker.io/ongres/postgres-util:v%s-build-%s";

  public PostgresUtil() {
  }

  @Override
  public Container getContainer(StackGresGeneratorContext context) {
    String pgVersion = StackGresComponents.calculatePostgresVersion(
        context.getClusterContext().getCluster().getSpec().getPostgresVersion());

    return new ContainerBuilder()
        .withName(NAME)
        .withImage(String.format(IMAGE_NAME, pgVersion,
            StackGresProperty.CONTAINER_BUILD.getString()))
        .withImagePullPolicy("IfNotPresent")
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh")
        .withArgs("-c", "while true; do sleep 10; done")
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.SOCKET
            .volumeMount(context.getClusterContext()))
        .build();
  }

}
