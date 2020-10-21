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
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMajorVersionUpgrade;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ResourceFactory;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@OpJob("majorVersionUpgrade")
public class DbOpsMajorVersionUpgradeJob extends DbOpsJob {

  @Inject
  public DbOpsMajorVersionUpgradeJob(
      ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityFactory,
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      LabelFactory<StackGresCluster> labelFactory,
      JsonMapper jsonMapper) {
    super(podSecurityFactory, clusterStatefulSetEnvironmentVariables, labelFactory, jsonMapper);
  }

  @Override
  protected String getOperation(StackGresDbOps dbOps) {
    return "major-version-upgrade";
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresClusterContext context, StackGresDbOps dbOps) {
    StackGresDbOpsMajorVersionUpgrade majorVersionUpgrade =
        dbOps.getSpec().getMajorVersionUpgrade();
    return ImmutableList.<EnvVar>builder()
        .add(
            new EnvVarBuilder()
                .withName("LINK")
                .withValue(Optional.ofNullable(majorVersionUpgrade)
                    .map(StackGresDbOpsMajorVersionUpgrade::getLink)
                    .map(String::valueOf)
                    .orElse("false"))
                .build(),
            new EnvVarBuilder()
                .withName("CLONE")
                .withValue(Optional.ofNullable(majorVersionUpgrade)
                    .map(StackGresDbOpsMajorVersionUpgrade::getClone)
                    .map(String::valueOf)
                    .orElse("false"))
                .build(),
            new EnvVarBuilder()
                .withName("CHECK")
                .withValue(Optional.ofNullable(majorVersionUpgrade)
                    .map(StackGresDbOpsMajorVersionUpgrade::getCheck)
                    .map(String::valueOf)
                    .orElse("false"))
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
                .build(),
            new EnvVarBuilder()
                .withName("MAJOR_VERSION_UPGRADE_CONTAINER_NAME")
                .withValue(StackgresClusterContainers.MAJOR_VERSION_UPGRADE)
                .build())
        .build();
  }

  @Override
  protected String getRunImage(StackGresClusterContext context) {
    return StackGresComponent.KUBECTL.findLatestImageName();
  }

  @Override
  protected ClusterStatefulSetPath getRunScript() {
    return ClusterStatefulSetPath.LOCAL_BIN_RUN_MAJOR_VERSION_UPGRADE_SH_PATH;
  }

  @Override
  protected boolean isExclusiveOp() {
    return true;
  }

}
