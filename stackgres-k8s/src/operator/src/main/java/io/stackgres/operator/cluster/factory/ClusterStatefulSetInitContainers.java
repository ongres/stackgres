/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
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
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
  }

  @Override
  public Stream<Container> streamResources(StackGresClusterContext context) {
    return Seq.of(
        setupArbitraryUser(context),
        createSetupDataPathsContainer(context),
        setupScriptsContainer(context));
  }

  private Container createSetupDataPathsContainer(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName("setup-data-paths")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_DATA_PATHS_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(context),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(context))
        .build();
  }

  private Container setupArbitraryUser(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName("setup-arbitrary-user")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(
            context, volumeMountBuilder -> volumeMountBuilder
                .withSubPath("etc")
                .withMountPath("/local/etc")
                .withReadOnly(false)))
        .build();
  }

  private Container setupScriptsContainer(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName("setup-scripts")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_SCRIPTS_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(context),
            ClusterStatefulSetVolumeConfig.LOCAL_BIN.volumeMount(context))
        .build();
  }

}
