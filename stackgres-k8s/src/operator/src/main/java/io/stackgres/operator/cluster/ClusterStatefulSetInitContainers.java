/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
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

  private final PatroniEnvironmentVariables patroniEnvironmentVariables;

  @Inject
  public ClusterStatefulSetInitContainers(PatroniEnvironmentVariables patroniEnvironmentVariables) {
    super();
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
  }

  @Override
  public Stream<Container> create(StackGresClusterContext config) {
    return Seq.of(Optional.of(createSetDataPermissionContainer(config)),
        Optional.of(createExecWithEnvContainer()),
        config.getRestoreContext()
        .map(restoreContext -> createRestoreEntrypointContainer(
            config, restoreContext, patroniEnvironmentVariables.list(config))))
        .filter(Optional::isPresent)
        .map(Optional::get);
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
      StackGresRestoreContext restoreContext, List<EnvVar> patroniSetEnvVariables) {
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
                .withName(PatroniConfigMap.name(config)).build())
            .build(),
            new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(RestoreConfigMap.name(config)).build())
            .build())
        .withEnv(ImmutableList.<EnvVar>builder()
            .addAll(patroniSetEnvVariables)
            .add(new EnvVarBuilder()
                .withName("RESTORE_BACKUP_ID")
                .withValue(restoreContext.getBackup().getStatus().getName())
                .build())
            .build())
        .withVolumeMounts(
            new VolumeMountBuilder()
                .withName(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME)
                .withMountPath("/restore")
                .build())
        .build();
  }

}
