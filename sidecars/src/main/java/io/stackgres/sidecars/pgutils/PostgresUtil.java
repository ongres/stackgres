/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgutils;

import java.util.List;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.StackGresClusterConfig;
import io.stackgres.common.StackGresSidecarTransformer;
import io.stackgres.common.StackGresUtil;

public class PostgresUtil implements StackGresSidecarTransformer<CustomResource> {

  private static final String NAME = "postgres-util";
  private static final String IMAGE_NAME = "docker.io/ongres/postgres-util:v%s-build-%s";

  public PostgresUtil() {}

  @Override
  public Container getContainer(StackGresClusterConfig config) {
    VolumeMount pgSocket = new VolumeMountBuilder()
        .withName("pg-socket")
        .withMountPath("/run/postgresql")
        .build();

    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(String.format(IMAGE_NAME,
            config.getCluster().getSpec().getPostgresVersion(), StackGresUtil.CONTAINER_BUILD))
        .withImagePullPolicy("Always")
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh")
        .withArgs("-c", "while true; do sleep 10; done")
        .withVolumeMounts(pgSocket);

    return container.build();
  }

  @Override
  public List<HasMetadata> getResources(StackGresClusterConfig config) {
    return ImmutableList.of();
  }

}
