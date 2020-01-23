/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgutils;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresSidecarTransformer;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.controller.ResourceGeneratorContext;

@Sidecar("postgres-util")
@Singleton
public class PostgresUtil
    implements StackGresSidecarTransformer<CustomResource, StackGresClusterContext> {

  private static final String NAME = "postgres-util";
  private static final String IMAGE_NAME = "docker.io/ongres/postgres-util:v%s-build-%s";

  public PostgresUtil() {
  }

  @Override
  public Container getContainer(ResourceGeneratorContext<StackGresClusterContext> context) {
    String pgVersion = StackGresComponents.calculatePostgresVersion(
        context.getContext().getCluster().getSpec().getPostgresVersion());

    List<VolumeMount> volumeMounts = new ArrayList<>();
    volumeMounts.add(new VolumeMountBuilder()
        .withName(ClusterStatefulSet.SOCKET_VOLUME_NAME)
        .withMountPath("/run/postgresql")
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

  @Override
  public List<HasMetadata> getResources(ResourceGeneratorContext<StackGresClusterContext> context) {
    return ImmutableList.of();
  }

}
