/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.stackgres.common.StackGresContext;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterStatefulSetInitContainers
    implements SubResourceStreamFactory<Container, StackGresClusterContext> {

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;

  public ClusterStatefulSetInitContainers(
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables) {
    super();
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
  }

  @Override
  public Stream<Container> streamResources(StackGresClusterContext config) {
    return Seq.of(Optional.of(createSetupDataPathsContainer(config)),
        Optional.of(setupScriptsContainer(config)),
        Optional.of(createSetupArbitraryUser(config)))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Container createSetupDataPathsContainer(StackGresClusterContext config) {
    return new ContainerBuilder()
        .withName("setup-data-paths")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withCommand("/bin/sh", "-ecx", Stream.of(
            "mkdir -p \"$PG_DATA_PATH\"",
            "chmod -R 700 \"$PG_DATA_PATH\"")
            .collect(Collectors.joining(" && ")))
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(config))
        .build();
  }

  private Container createSetupArbitraryUser(StackGresClusterContext config) {
    return new ContainerBuilder()
        .withName("setup-arbitrary-user")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withCommand("/bin/sh", "-ecx", Seq.of(
            "USER=postgres",
            "UID=$(id -u)",
            "GID=$(id -g)",
            "SHELL=/bin/sh",
            "cp \"$TEMPLATES_PATH/passwd\" /local/etc/.",
            "cp \"$TEMPLATES_PATH/group\" /local/etc/.",
            "cp \"$TEMPLATES_PATH/shadow\" /local/etc/.",
            "cp \"$TEMPLATES_PATH/gshadow\" /local/etc/.",
            "echo \"$USER:x:$UID:$GID::$PG_BASE_PATH:$SHELL\" >> /local/etc/passwd",
            "chmod 644 /local/etc/passwd",
            "echo \"$USER:x:$GID:\" >> /local/etc/group",
            "chmod 644 /local/etc/group",
            "echo \"$USER\"':!!:18179:0:99999:7:::' >> /local/etc/shadow",
            "chmod 000 /local/etc/shadow",
            "echo \"$USER\"':!::' >> /local/etc/gshadow",
            "chmod 000 /local/etc/gshadow")
            .collect(Collectors.joining(" && ")))
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(config),
            ClusterStatefulSetVolumeConfig.LOCAL.volumeMount(
            config, volumeMountBuilder -> volumeMountBuilder
                .withSubPath("etc")
                .withMountPath("/local/etc")
                .withReadOnly(false)))
        .build();
  }

  private Container setupScriptsContainer(StackGresClusterContext config) {
    return new ContainerBuilder()
        .withName("setup-scripts")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withCommand("/bin/sh", "-ecx", Seq.of(
            "cp $TEMPLATES_PATH/start-patroni.sh \"$LOCAL_BIN_PATH\"",
            "cp $TEMPLATES_PATH/start-patroni-with-restore.sh \"$LOCAL_BIN_PATH\"",
            "cp $TEMPLATES_PATH/post-init.sh \"$LOCAL_BIN_PATH\"",
            "cp $TEMPLATES_PATH/exec-with-env \"$LOCAL_BIN_PATH\"",
            "sed -i \"s#\\${POSTGRES_PORT}#${POSTGRES_PORT}#g\""
                + " \"$LOCAL_BIN_PATH/post-init.sh\"",
            "sed -i \"s#\\${BASE_ENV_PATH}#${BASE_ENV_PATH}#g\""
                + " \"$LOCAL_BIN_PATH/exec-with-env\"",
            "sed -i \"s#\\${BASE_SECRET_PATH}#${BASE_SECRET_PATH}#g\""
                + " \"$LOCAL_BIN_PATH/exec-with-env\"",
            "chmod a+x \"$LOCAL_BIN_PATH/start-patroni.sh\"",
            "chmod a+x \"$LOCAL_BIN_PATH/start-patroni-with-restore.sh\"",
            "chmod a+x \"$LOCAL_BIN_PATH/post-init.sh\"",
            "chmod a+x \"$LOCAL_BIN_PATH/exec-with-env\"")
            .collect(Collectors.joining(" && ")))
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(config),
            ClusterStatefulSetVolumeConfig.LOCAL.volumeMount(
                ClusterStatefulSetPath.LOCAL_BIN_PATH, config))
        .build();
  }

}
