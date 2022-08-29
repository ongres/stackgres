/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgutils;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;

@Sidecar(StackGresContainer.POSTGRES_UTIL)
@Singleton
@OperatorVersionBinder
@RunningContainer(StackGresContainer.POSTGRES_UTIL)
public class PostgresUtil extends AbstractPostgresUtil {

  private ContainerUserOverrideMounts containerUserOverrideMounts;

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
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
                .withName(PatroniStaticVolume.EMPTY_BASE.getVolumeName())
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
