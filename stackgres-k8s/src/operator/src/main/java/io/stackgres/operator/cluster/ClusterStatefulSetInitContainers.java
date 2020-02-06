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
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.patroni.PatroniConfigMap;

import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class ClusterStatefulSetInitContainers {

  private final ClusterStatefulSetEnvironmentVariables environmentVariablesFactory;

  @Inject
  public ClusterStatefulSetInitContainers(
      ClusterStatefulSetEnvironmentVariables environmentVariablesFactory) {
    this.environmentVariablesFactory = environmentVariablesFactory;
  }

  public ImmutableList<Container> getInitContainers(StackGresClusterContext config) {
    ImmutableList<EnvVar> patroniSetEnvVariables = environmentVariablesFactory
        .getPatroniEnvironmentVariables(config);
    ImmutableList<EnvVar> restoreSetEnvVariables = environmentVariablesFactory
        .getRestoreEnvironmentVariables(config);

    return Seq.of(Optional.of(createSetDataPermissionContainer(config)),
        Optional.of(createExecWithEnvContainer()),
        config.getRestoreContext()
        .map(restoreContext -> createRestoreEntrypointContainer(
            config, patroniSetEnvVariables, restoreSetEnvVariables)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(ImmutableList.toImmutableList());
  }

  private Container createSetDataPermissionContainer(StackGresClusterContext config) {
    return new ContainerBuilder()
        .withName("set-data-permissions")
        .withImage("busybox")
        .withCommand("/bin/sh", "-ecx", getSetDataPermissionCommand())
        .withVolumeMounts(getSetDataPermissionVolumeMounts(config))
        .build();
  }

  private VolumeMount[] getSetDataPermissionVolumeMounts(StackGresClusterContext config) {
    return Stream.of(
        Stream.of(new VolumeMountBuilder()
            .withName(config.getCluster().getMetadata().getName()
                + ClusterStatefulSet.DATA_SUFFIX)
            .withMountPath(ClusterStatefulSet.PG_VOLUME_PATH)
            .build()))
        .flatMap(s -> s)
        .toArray(VolumeMount[]::new);
  }

  private String getSetDataPermissionCommand() {
    return Stream.of(
        Stream.of(
            "chmod -R 700 " + ClusterStatefulSet.PG_VOLUME_PATH,
            "chown -R 999:999 " + ClusterStatefulSet.PG_VOLUME_PATH))
        .flatMap(s -> s)
        .collect(Collectors.joining(" && "));
  }

  private Container createExecWithEnvContainer() {
    return new ContainerBuilder()
        .withName("exec-with-env")
        .withImage("busybox")
        .withCommand("/bin/sh", "-ecx", Unchecked.supplier(() -> Resources
            .asCharSource(
                ClusterStatefulSet.class.getResource("/create-exec-with-env.sh"),
                StandardCharsets.UTF_8)
            .read()).get())
        .build();
  }

  private Container createRestoreEntrypointContainer(StackGresClusterContext config,
      ImmutableList<EnvVar> patroniSetEnvVariables, ImmutableList<EnvVar> restoreSetEnvVariables) {
    return new ContainerBuilder()
        .withName("restore-entrypoint")
        .withImage("busybox")
        .withCommand("/bin/sh", "-ecx", Unchecked.supplier(() -> Resources
            .asCharSource(
                ClusterStatefulSet.class.getResource("/restore-entrypoint.sh"),
                StandardCharsets.UTF_8)
            .read()).get())
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(PatroniConfigMap.patroniName(config)).build())
            .build(),
            new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(BackupConfigMap.restoreName(config)).build())
            .build())
        .withEnv(ImmutableList.<EnvVar>builder()
            .addAll(patroniSetEnvVariables)
            .addAll(restoreSetEnvVariables)
            .build())
        .withVolumeMounts(
            new VolumeMountBuilder()
                .withName(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME)
                .withMountPath("/restore")
                .build())
        .build();
  }

}
