/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;
import static io.stackgres.common.StackGresShardedClusterUtil.getShardClusterName;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.ShardedClusterPath;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsRestart;
import io.stackgres.common.labels.LabelFactoryForShardedDbOps;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
@ShardedDbOpsJob("restart")
public class ShardedDbOpsRestartJob extends AbstractShardedDbOpsJob {

  @Inject
  public ShardedDbOpsRestartJob(
      ResourceFactory<StackGresShardedDbOpsContext, PodSecurityContext> podSecurityFactory,
      ShardedDbOpsEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      LabelFactoryForShardedDbOps dbOpsLabelFactory,
      ObjectMapper jsonMapper,
      KubectlUtil kubectl,
      ShardedDbOpsVolumeMounts dbOpsVolumeMounts,
      ShardedDbOpsTemplatesVolumeFactory dbOpsTemplatesVolumeFactory) {
    super(podSecurityFactory, clusterStatefulSetEnvironmentVariables,
        dbOpsLabelFactory, jsonMapper, kubectl, dbOpsVolumeMounts, dbOpsTemplatesVolumeFactory);
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresShardedDbOpsContext context) {
    StackGresShardedDbOps dbOps = context.getSource();
    StackGresShardedDbOpsRestart restart =
        dbOps.getSpec().getRestart();
    return ImmutableList.<EnvVar>builder()
        .add(
            new EnvVarBuilder()
            .withName("METHOD")
            .withValue(Optional.ofNullable(restart)
                .map(StackGresShardedDbOpsRestart::getMethod)
                .orElse(DbOpsMethodType.IN_PLACE.toString()))
            .build(),
            new EnvVarBuilder()
            .withName("ONLY_PENDING_RESTART")
            .withValue(Optional.ofNullable(restart)
                .map(StackGresShardedDbOpsRestart::getOnlyPendingRestart)
                .map(String::valueOf)
                .orElse("false"))
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_NAMES")
            .withValue(Seq.of(getCoordinatorClusterName(context.getShardedCluster()))
                .filter(ignore -> !StackGresShardingType.SHARDING_SPHERE.equals(
                    StackGresShardingType.fromString(
                        context.getShardedCluster().getSpec().getType())))
                .append(Seq.range(0, context.getShardedCluster()
                    .getSpec().getShards().getClusters())
                    .map(index -> getShardClusterName(context.getShardedCluster(), index)))
                .toString(" "))
            .build(),
            new EnvVarBuilder()
            .withName("DBOPS_LABELS")
            .withValue(dbOpsLabelFactory.shardedDbOpsLabels(context.getSource())
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(",")))
            .build(),
            new EnvVarBuilder()
            .withName("DBOPS_LABELS_JSON")
            .withValue(jsonMapper.valueToTree(
                dbOpsLabelFactory.shardedDbOpsLabels(context.getSource()))
                .toString())
            .build(),
            new EnvVarBuilder()
            .withName("CRD_GROUP")
            .withValue(CommonDefinition.GROUP)
            .build(),
            new EnvVarBuilder()
            .withName("SHARDED_CLUSTER_CRD_NAME")
            .withValue(HasMetadata.getPlural(StackGresShardedCluster.class))
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_NAMESPACE")
            .withValue(context.getSource().getMetadata().getNamespace())
            .build(),
            new EnvVarBuilder()
            .withName("SHARDED_CLUSTER_NAME")
            .withValue(context.getSource().getSpec().getSgShardedCluster())
            .build(),
            new EnvVarBuilder()
            .withName("SERVICE_ACCOUNT")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "spec.serviceAccountName"))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName("POD_NAME")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName("SHARDED_DBOPS_CRD_NAME")
            .withValue(CustomResource.getCRDName(StackGresShardedDbOps.class))
            .build(),
            new EnvVarBuilder()
            .withName("SHARDED_DBOPS_CRD_KIND")
            .withValue(HasMetadata.getKind(StackGresShardedDbOps.class))
            .build(),
            new EnvVarBuilder()
            .withName("SHARDED_DBOPS_CRD_APIVERSION")
            .withValue(HasMetadata.getApiVersion(StackGresShardedDbOps.class))
            .build(),
            new EnvVarBuilder()
            .withName("SHARDED_DBOPS_NAME")
            .withValue(dbOps.getMetadata().getName())
            .build(),
            new EnvVarBuilder()
            .withName("SHARDED_DBOPS_UID")
            .withValue(dbOps.getMetadata().getUid())
            .build(),
            new EnvVarBuilder()
            .withName("DBOPS_CRD_NAME")
            .withValue(CustomResource.getCRDName(StackGresDbOps.class))
            .build(),
            new EnvVarBuilder()
            .withName("DBOPS_CRD_KIND")
            .withValue(HasMetadata.getKind(StackGresDbOps.class))
            .build(),
            new EnvVarBuilder()
            .withName("DBOPS_CRD_APIVERSION")
            .withValue(HasMetadata.getApiVersion(StackGresDbOps.class))
            .build(),
            new EnvVarBuilder()
            .withName("DBOPS_COMPLETED")
            .withValue(DbOpsStatusCondition.Type.COMPLETED.getType())
            .build(),
            new EnvVarBuilder()
            .withName("DBOPS_FAILED")
            .withValue(DbOpsStatusCondition.Type.FAILED.getType())
            .build(),
            new EnvVarBuilder()
            .withName("POSTGRES_VERSION_KEY")
            .withValue(StackGresContext.POSTGRES_VERSION_KEY)
            .build(),
            new EnvVarBuilder()
            .withName("LOCK_DURATION")
            .withValue(OperatorProperty.LOCK_DURATION.getString())
            .build(),
            new EnvVarBuilder()
            .withName("LOCK_SLEEP")
            .withValue(OperatorProperty.LOCK_POLL_INTERVAL.getString())
            .build(),
            new EnvVarBuilder()
            .withName("LOCK_SERVICE_ACCOUNT_KEY")
            .withValue(StackGresContext.LOCK_SERVICE_ACCOUNT_KEY)
            .build(),
            new EnvVarBuilder()
            .withName("LOCK_POD_KEY")
            .withValue(StackGresContext.LOCK_POD_KEY)
            .build(),
            new EnvVarBuilder()
            .withName("LOCK_TIMEOUT_KEY")
            .withValue(StackGresContext.LOCK_TIMEOUT_KEY)
            .build())
        .build();
  }

  @Override
  protected String getRunImage(StackGresShardedDbOpsContext context) {
    return kubectl.getImageName(context.getShardedCluster());
  }

  @Override
  protected ShardedClusterPath getRunScript() {
    return ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_RESTART_SH_PATH;
  }

  @Override
  protected boolean isExclusiveOp() {
    return true;
  }

}
