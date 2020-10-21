/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.dbops;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestart;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ResourceFactory;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@OpJob("restart")
public class DbOpsRestartJob extends DbOpsJob {

  @Inject
  public DbOpsRestartJob(
      ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityFactory,
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      LabelFactory<StackGresCluster> labelFactory,
      JsonMapper jsonMapper) {
    super(podSecurityFactory, clusterStatefulSetEnvironmentVariables, labelFactory, jsonMapper);
  }

  public DbOpsRestartJob() {
    super(null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected boolean isExclusiveOp() {
    return true;
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresClusterContext context, StackGresDbOps dbOps) {
    StackGresDbOpsRestart restart =
        dbOps.getSpec().getRestart();
    List<EnvVar> runEnvVars = ImmutableList.<EnvVar>builder()
        .add(
            new EnvVarBuilder()
                .withName("REDUCED_IMPACT")
                .withValue(Optional.ofNullable(restart)
                    .map(StackGresDbOpsRestart::isMethodReducedImpact)
                    .map(String::valueOf)
                    .orElse("true"))
                .build(),
            new EnvVarBuilder()
                .withName("RESTART_PRIMARY_FIRST")
                .withValue(Optional.ofNullable(restart)
                    .map(StackGresDbOpsRestart::getRestartPrimaryFirst)
                    .map(String::valueOf)
                    .orElse("true"))
                .build(),
            new EnvVarBuilder()
                .withName("CLUSTER_CRD_NAME")
                .withValue(CustomResource.getCRDName(StackGresCluster.class))
                .build(),
            new EnvVarBuilder()
                .withName("CLUSTER_NAMESPACE")
                .withValue(context.getSource().getMetadata().getNamespace())
                .build(),
            new EnvVarBuilder()
                .withName("CLUSTER_NAME")
                .withValue(context.getSource().getMetadata().getName())
                .build(),
            new EnvVarBuilder()
                .withName("POD_NAME")
                .withValueFrom(new EnvVarSourceBuilder()
                    .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                    .build())
                .build(),
            new EnvVarBuilder()
                .withName("DB_OPS_CRD_NAME")
                .withValue(CustomResource.getCRDName(StackGresDbOps.class))
                .build(),
            new EnvVarBuilder()
                .withName("DB_OPS_NAME")
                .withValue(dbOps.getMetadata().getName())
                .build(),
            new EnvVarBuilder()
                .withName("CLUSTER_POD_LABELS")
                .withValue(labelFactory.patroniClusterLabels(context.getSource())
                    .entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(",")))
                .build(),
            new EnvVarBuilder()
                .withName("CLUSTER_PRIMARY_POD_LABELS")
                .withValue(labelFactory.patroniPrimaryLabels(context.getSource())
                    .entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(",")))
                .build(),
            new EnvVarBuilder()
                .withName("PATRONI_CONTAINER_NAME")
                .withValue(StackgresClusterContainers.PATRONI)
                .build())
        .build();
    return runEnvVars;
  }

  @Override
  protected String getRunImage(StackGresClusterContext context) {
    return StackGresComponent.KUBECTL.findLatestImageName();
  }

  @Override
  protected ClusterStatefulSetPath getRunScript() {
    return ClusterStatefulSetPath.LOCAL_BIN_RUN_RESTART_SH_PATH;
  }

}
