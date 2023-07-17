/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgutils;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Sidecar(StackGresContainer.POSTGRES_UTIL)
@Singleton
@OperatorVersionBinder
@RunningContainer(StackGresContainer.POSTGRES_UTIL)
public class PostgresUtil extends AbstractPostgresUtil {

  private ContainerUserOverrideMounts containerUserOverrideMounts;

  @Override
  public Container getContainer(ClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresContainer.POSTGRES_UTIL.getName())
        .withImage(StackGresComponent.POSTGRES_UTIL.get(context.getClusterContext().getCluster())
            .getImageName(
                context.getClusterContext().getSource().getSpec().getPostgres().getVersion()))
        .withImagePullPolicy("IfNotPresent")
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh")
        .withArgs("-c", "while true; do sleep 10; done")
        .addAllToVolumeMounts(postgresSocket.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
                .withName(StackGresVolume.EMPTY_BASE.getName())
                .withMountPath("/var/lib/postgresql")
                .build()
        )
        .addAllToVolumeMounts(containerUserOverrideMounts.getVolumeMounts(context))
        .build();
  }

  @Inject
  public void setContainerUserOverrideMounts(
      ContainerUserOverrideMounts containerUserOverrideMounts) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
  }

}
