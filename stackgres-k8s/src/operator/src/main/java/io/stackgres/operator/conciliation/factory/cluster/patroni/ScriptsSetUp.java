/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@InitContainer(order = 1)
public class ScriptsSetUp implements ContainerFactory<StackGresClusterContext> {

  private final ClusterEnvironmentVariablesFactoryDiscoverer<StackGresClusterContext>
      clusterEnvVarFactoryDiscoverer;

  @Inject
  public ScriptsSetUp(
      ClusterEnvironmentVariablesFactoryDiscoverer<StackGresClusterContext>
          clusterEnvVarFactoryDiscoverer) {
    this.clusterEnvVarFactoryDiscoverer = clusterEnvVarFactoryDiscoverer;
  }

  @Override
  public Container getContainer(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName("setup-scripts")
        .withImage(StackGresComponent.KUBECTL.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_SCRIPTS_SH_PATH.filename())
        .withEnv(getClusterEnvVars(context))
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(context))
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

  @NotNull
  private List<EnvVar> getClusterEnvVars(StackGresClusterContext context) {
    List<EnvVar> clusterEnvVars = new ArrayList<>();

    List<ClusterEnvironmentVariablesFactory<StackGresClusterContext>> clusterEnvVarFactories =
        clusterEnvVarFactoryDiscoverer.discoverFactories(context);

    clusterEnvVarFactories.forEach(envVarFactory ->
        clusterEnvVars.addAll(envVarFactory.buildEnvironmentVariables(context)));
    return clusterEnvVars;
  }

}
