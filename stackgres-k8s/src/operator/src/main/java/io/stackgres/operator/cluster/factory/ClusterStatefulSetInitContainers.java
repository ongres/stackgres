/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

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
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresRestoreContext;
import io.stackgres.operator.patroni.factory.PatroniConfigMap;
import io.stackgres.operator.patroni.factory.PatroniEnvironmentVariables;
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
    return Seq.of(Optional.of(createSetupDataPathsContainer(config)),
        Optional.of(createExecWithEnvContainer(config)),
        config.getRestoreContext()
        .map(restoreContext -> createRestoreEntrypointContainer(config, restoreContext)))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Container createSetupDataPathsContainer(StackGresClusterContext config) {
    return new ContainerBuilder()
        .withName("setup-data-paths")
        .withImage("busybox")
        .withCommand("/bin/sh", "-ecx", Stream.of(
            "mkdir -p " + ClusterStatefulSetPath.PG_DATA_PATH.path(),
            "chmod -R 700 " + ClusterStatefulSetPath.PG_DATA_PATH.path())
            .collect(Collectors.joining(" && ")))
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(config))
        .build();
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
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.LOCAL_BIN.volumeMount(config))
        .build();
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
                .withValue(restoreContext.getBackup().getStatus().getInternalName())
                .build())
            .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.RESTORE_ENTRYPOINT.volumeMount(config))
        .build();
  }

}
