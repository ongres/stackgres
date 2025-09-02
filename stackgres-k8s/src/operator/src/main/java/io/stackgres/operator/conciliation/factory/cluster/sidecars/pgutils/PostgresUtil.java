/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgutils;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.PostgresSocketMounts;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.PostgresEnvironmentVariables;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Sidecar(StackGresContainer.POSTGRES_UTIL)
@Singleton
@OperatorVersionBinder
@RunningContainer(StackGresContainer.POSTGRES_UTIL)
public class PostgresUtil implements ContainerFactory<ClusterContainerContext> {

  private final PostgresEnvironmentVariables postgresEnvironmentVariables;
  private final PostgresSocketMounts postgresSocketMounts;
  private final UserOverrideMounts userOverrideMounts;

  @Inject
  public PostgresUtil(
      PostgresEnvironmentVariables postgresEnvironmentVariables,
      PostgresSocketMounts postgresSocketMounts,
      UserOverrideMounts userOverrideMounts) {
    this.postgresEnvironmentVariables = postgresEnvironmentVariables;
    this.postgresSocketMounts = postgresSocketMounts;
    this.userOverrideMounts = userOverrideMounts;
  }

  @Override
  public boolean isActivated(ClusterContainerContext context) {
    return !Optional.of(context.getClusterContext().getCluster().getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisablePostgresUtil)
        .orElse(false);
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        getPostgresFlavorComponent(context.getClusterContext().getCluster())
        .get(context.getClusterContext().getCluster())
        .getVersion(
            context.getClusterContext().getCluster().getStatus().getPostgresVersion()));
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresContainer.POSTGRES_UTIL.getName())
        .withImage(StackGresComponent.POSTGRES_UTIL.get(context.getClusterContext().getCluster())
            .getImageName(
                context.getClusterContext().getSource().getStatus().getPostgresVersion()))
        .withImagePullPolicy(getDefaultPullPolicy())
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh")
        .withArgs("-c", "while true; do sleep 10; done")
        .addAllToVolumeMounts(postgresSocketMounts.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
                .withName(StackGresVolume.EMPTY_BASE.getName())
                .withMountPath("/var/lib/postgresql")
                .build()
        )
        .addAllToVolumeMounts(userOverrideMounts.getVolumeMounts(context))
        .addAllToEnv(postgresEnvironmentVariables.getEnvVars(context.getClusterContext()))
        .build();
  }

}
