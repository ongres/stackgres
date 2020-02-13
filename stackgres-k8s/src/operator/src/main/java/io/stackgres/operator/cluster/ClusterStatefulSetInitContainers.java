/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.cluster.ClusterStatefulSet.ClusterStatefulSetPaths;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresRestoreContext;
import io.stackgres.operator.patroni.PatroniConfigMap;
import io.stackgres.operator.patroni.PatroniEnvironmentVariables;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;

import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class ClusterStatefulSetInitContainers
    implements SubResourceStreamFactory<Container, StackGresClusterContext> {

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;
  private final PatroniEnvironmentVariables patroniEnvironmentVariables;

  public ClusterStatefulSetInitContainers(
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      PatroniEnvironmentVariables patroniEnvironmentVariables) {
    super();
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
  }

  @Override
  public Stream<Container> streamResources(StackGresClusterContext config) {
    return Seq.of(Optional.of(createSetDataPermissionContainer(config)),
        Optional.of(createExecWithEnvContainer(config)),
        config.getRestoreContext()
        .map(restoreContext -> createRestoreEntrypointContainer(config, restoreContext)))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Container createSetDataPermissionContainer(StackGresClusterContext config) {
    return new ContainerBuilder()
        .withName("set-data-permissions")
        .withImage("busybox")
        .withCommand("/bin/sh", "-ecx", Stream.of(
            "chmod -R 700 " + ClusterStatefulSetPaths.PG_BASE_PATH.path(),
            "chown -R 999:999 " + ClusterStatefulSetPaths.PG_BASE_PATH.path())
            .collect(Collectors.joining(" && ")))
        .withVolumeMounts(getSetDataPermissionVolumeMounts(config))
        .build();
  }

  private VolumeMount[] getSetDataPermissionVolumeMounts(StackGresClusterContext config) {
    return Stream.of(
        Stream.of(new VolumeMountBuilder()
            .withName(ClusterStatefulSet.dataName(config))
            .withMountPath(ClusterStatefulSetPaths.PG_BASE_PATH.path())
            .build()))
        .flatMap(s -> s)
        .toArray(VolumeMount[]::new);
  }

  private Container createExecWithEnvContainer(StackGresClusterContext config) {
    return new ContainerBuilder()
        .withName("exec-with-env")
        .withImage("busybox")
        .withCommand("/bin/sh", "-ecx", Unchecked.supplier(() -> Resources
            .asCharSource(
                ClusterStatefulSet.class.getResource("/create-exec-with-env.sh"),
                StandardCharsets.UTF_8)
            .read()).get())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
        .withVolumeMounts(getExecWithEnvVolumeMounts())
        .build();
  }

  private VolumeMount[] getExecWithEnvVolumeMounts() {
    return Stream.of(
        Stream.of(new VolumeMountBuilder()
            .withName(ClusterStatefulSet.LOCAL_BIN_VOLUME_NAME)
            .withMountPath(ClusterStatefulSetPaths.LOCAL_BIN_PATH.path())
            .build()))
        .flatMap(s -> s)
        .toArray(VolumeMount[]::new);
  }

  private Container createRestoreEntrypointContainer(StackGresClusterContext config,
      StackGresRestoreContext restoreContext) {
    return new ContainerBuilder()
        .withName("restore-entrypoint")
        .withImage("busybox")
        .withCommand("/bin/sh", "-ecx", Unchecked.supplier(() -> Resources
            .asCharSource(
                ClusterStatefulSet.class.getResource("/create-restore-entrypoint.sh"),
                StandardCharsets.UTF_8)
            .read()).get())
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(PatroniConfigMap.name(config)).build())
            .build(),
            new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(RestoreConfigMap.name(config)).build())
            .build())
        .withEnv(ImmutableList.<EnvVar>builder()
            .addAll(clusterStatefulSetEnvironmentVariables.listResources(config))
            .addAll(patroniEnvironmentVariables.listResources(config))
            .add(new EnvVarBuilder()
                .withName("RESTORE_BACKUP_ID")
                .withValue(restoreContext.getBackup().getStatus().getName())
                .build())
            .build())
        .withVolumeMounts(
            new VolumeMountBuilder()
                .withName(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME_NAME)
                .withMountPath(ClusterStatefulSetPaths.RESTORE_ENTRYPOINT_PATH.path())
                .build())
        .build();
  }

}
