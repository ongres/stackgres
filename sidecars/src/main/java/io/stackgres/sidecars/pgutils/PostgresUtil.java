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
import io.stackgres.common.sgcluster.StackGresCluster;
import io.stackgres.sidecars.Sidecar;

public class PostgresUtil implements Sidecar {

  private static final String NAME = "postgres-util";
  private static final String IMAGE = "docker.io/ongres/postgres-util:11.5";

  public PostgresUtil() {}

  @Override
  public Container create(StackGresCluster resource) {
    VolumeMount pgSocket = new VolumeMountBuilder()
        .withName("pg-socket")
        .withMountPath("/run/postgresql")
        .build();

    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(IMAGE)
        .withImagePullPolicy("Always")
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh")
        .withArgs("-c", "while true; do sleep 10; done")
        .withVolumeMounts(pgSocket);

    return container.build();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<HasMetadata> createDependencies(StackGresCluster resource) {
    return ImmutableList.of();
  }

}
