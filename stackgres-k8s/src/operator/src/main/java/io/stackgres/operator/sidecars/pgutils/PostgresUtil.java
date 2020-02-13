/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgutils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.cluster.ClusterStatefulSet.ClusterStatefulSetPaths;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;

@Sidecar("postgres-util")
@Singleton
public class PostgresUtil implements StackGresClusterSidecarResourceFactory<Void> {

  private static final String NAME = "postgres-util";
  private static final String IMAGE_NAME = "docker.io/ongres/postgres-util:v%s-build-%s";

  public PostgresUtil() {
  }

  @Override
  public Container getContainer(StackGresGeneratorContext context) {
    String pgVersion = StackGresComponents.calculatePostgresVersion(
        context.getClusterContext().getCluster().getSpec().getPostgresVersion());

    List<VolumeMount> volumeMounts = new ArrayList<>();
    volumeMounts.add(new VolumeMountBuilder()
        .withName(ClusterStatefulSet.SOCKET_VOLUME_NAME)
        .withMountPath(ClusterStatefulSetPaths.PG_RUN_PATH.path())
        .build());

    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(String.format(IMAGE_NAME, pgVersion, StackGresUtil.CONTAINER_BUILD))
        .withImagePullPolicy("Always")
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh")
        .withArgs("-c", "while true; do sleep 10; done")
        .withVolumeMounts(volumeMounts);

    return container.build();
  }

}
