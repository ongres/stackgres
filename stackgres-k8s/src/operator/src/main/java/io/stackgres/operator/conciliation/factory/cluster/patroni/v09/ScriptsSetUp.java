/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v09;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterEnvironmentVariablesFactory;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterEnvironmentVariablesFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterStatefulSetVolumeConfig;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V093)
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
        .withEnv(getClusterEnvVars(context))
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(
                ClusterStatefulSetPath.LOCAL_BIN_PATH, context))
        .build();
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContext context) {
    return Map.of();
  }

  @Override
  public List<Volume> getVolumes(StackGresClusterContext context) {
    return List.of();
  }

  private List<EnvVar> getClusterEnvVars(StackGresClusterContext context) {
    List<EnvVar> clusterEnvVars = new ArrayList<>();

    List<ClusterEnvironmentVariablesFactory<StackGresClusterContext>> clusterEnvVarFactories =
        clusterEnvVarFactoryDiscoverer.discoverFactories(context);

    clusterEnvVarFactories.forEach(envVarFactory ->
        clusterEnvVars.addAll(envVarFactory.buildEnvironmentVariables(context)));
    return clusterEnvVars;
  }

}
