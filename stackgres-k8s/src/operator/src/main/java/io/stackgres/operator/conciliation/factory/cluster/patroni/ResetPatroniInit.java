/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@InitContainer(order = 4)
public class ResetPatroniInit implements ContainerFactory<StackGresClusterContext> {

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;

  private final PatroniServices patroniServices;

  @Inject
  public ResetPatroniInit(
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      @OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
      PatroniServices patroniServices) {
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
    this.patroniServices = patroniServices;
  }

  @Override
  public boolean isActivated(StackGresClusterContext context) {
    return Optional.of(context.getSource())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade).isPresent();
  }

  @Override
  public Container getContainer(StackGresClusterContext context) {

    String primaryInstance = Optional.of(context.getSource())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .map(StackGresClusterDbOpsMajorVersionUpgradeStatus::getPrimaryInstance)
        .orElseThrow();

    return
        new ContainerBuilder()
            .withName("reset-patroni-initialize")
            .withImage(StackGresComponent.KUBECTL.findLatestImageName())
            .withImagePullPolicy("IfNotPresent")
            .withCommand("/bin/sh", "-ex",
                ClusterStatefulSetPath.TEMPLATES_PATH.path()
                    + "/"
                    + ClusterStatefulSetPath.LOCAL_BIN_RESET_PATRONI_INITIALIZE_SH_PATH.filename())
            .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context.getSource()))
            .addToEnv(
                new EnvVarBuilder()
                    .withName("PRIMARY_INSTANCE")
                    .withValue(primaryInstance)
                    .build(),
                new EnvVarBuilder()
                    .withName("POD_NAME")
                    .withValueFrom(new EnvVarSourceBuilder()
                        .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                        .build())
                    .build(),
                new EnvVarBuilder()
                    .withName("CLUSTER_NAMESPACE")
                    .withValue(context.getCluster().getMetadata().getNamespace())
                    .build(),
                new EnvVarBuilder()
                    .withName("PATRONI_ENDPOINT_NAME")
                    .withValue(patroniServices.configName(context))
                    .build())
            .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(context),
                ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
                ClusterStatefulSetVolumeConfig.USER.volumeMount(context),
                ClusterStatefulSetVolumeConfig.LOCAL_BIN.volumeMount(context))
            .build();
  }

  @Override
  public List<Volume> getVolumes(StackGresClusterContext context) {
    return List.of();
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContext context) {
    return Map.of();
  }
}
