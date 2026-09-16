/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import static io.stackgres.common.StackGresShardedClusterUtil.coordinatorConfigName;
import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;
import static io.stackgres.common.StackGresShardedClusterUtil.getQueryRouterClusterName;
import static io.stackgres.common.StackGresShardedClusterUtil.getWorkerClusterName;
import static io.stackgres.common.StackGresShardedClusterUtil.queryRouterConfigName;
import static io.stackgres.common.StackGresShardedClusterUtil.workerConfigName;

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
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsMajorVersionUpgrade;
import io.stackgres.common.labels.LabelFactoryForShardedDbOps;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
@ShardedDbOpsJob("majorVersionUpgrade")
public class ShardedDbOpsMajorVersionUpgradeJob extends AbstractShardedDbOpsJob {

  @Inject
  public ShardedDbOpsMajorVersionUpgradeJob(
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
    StackGresShardedDbOpsMajorVersionUpgrade majorVersionUpgrade =
        dbOps.getSpec().getMajorVersionUpgrade();
    final StackGresShardedCluster cluster = context.getShardedCluster();
    final String targetPostgresVersion = Optional.ofNullable(majorVersionUpgrade)
        .map(StackGresShardedDbOpsMajorVersionUpgrade::getPostgresVersion)
        .orElseThrow();
    final boolean isCitus = StackGresShardingType.CITUS.equals(
        StackGresShardingType.fromString(cluster.getSpec().getType()));
    final String rawSgPostgresConfig = Optional.ofNullable(majorVersionUpgrade)
        .map(StackGresShardedDbOpsMajorVersionUpgrade::getSgPostgresConfig)
        .orElseThrow();
    final int workerClusters = cluster.getSpec().getWorkers().getClusters();
    final int queryRouterClusters = Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getQueryRouterClusters)
        .orElse(0);
    final List<String> clusterNames = Seq.of(getCoordinatorClusterName(cluster))
        .filter(ignore -> !StackGresShardingType.SHARDING_SPHERE.equals(
            StackGresShardingType.fromString(cluster.getSpec().getType())))
        .append(Seq.range(0, workerClusters)
            .map(index -> getWorkerClusterName(cluster, index)))
        .append(Seq.range(0, queryRouterClusters)
            .map(index -> getQueryRouterClusterName(cluster, String.valueOf(index))))
        .toList();
    // The child SGDbOps each upgrade a child SGCluster, so each must reference the SGPostgresConfig
    // that the target child SGCluster will actually use. The actual name is provided by the
    // coordinatorConfigName/workerConfigName/queryRouterConfigName functions (for citus a per-cluster
    // config named "<childClusterName>-<major>" with citus/pg_cron injected; for other sharding types
    // the configured SGPostgresConfig). Aligned by position with CLUSTER_NAMES.
    final String clusterSgPostgresConfigs;
    if (isCitus) {
      clusterSgPostgresConfigs = Seq.of(coordinatorConfigName(cluster, targetPostgresVersion))
          .append(Seq.range(0, workerClusters)
              .map(index -> workerConfigName(cluster, index, targetPostgresVersion)))
          .append(Seq.range(0, queryRouterClusters)
              .map(index -> queryRouterConfigName(cluster, index, targetPostgresVersion)))
          .toString(" ");
    } else {
      clusterSgPostgresConfigs = clusterNames.stream()
          .map(ignore -> rawSgPostgresConfig)
          .collect(Collectors.joining(" "));
    }
    return ImmutableList.<EnvVar>builder()
        .add(
            new EnvVarBuilder()
            .withName("TARGET_POSTGRES_VERSION")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getPostgresVersion)
                .orElseThrow())
            .build(),
            new EnvVarBuilder()
            .withName("SOURCE_POSTGRES_VERSION")
            .withValue(Optional.ofNullable(cluster.getStatus())
                .map(StackGresShardedClusterStatus::getPostgresVersion)
                .orElse(cluster.getSpec().getPostgres().getVersion()))
            .build(),
            new EnvVarBuilder()
            .withName("SG_POSTGRES_CONFIG")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getSgPostgresConfig)
                .orElseThrow())
            .build(),
            new EnvVarBuilder()
            .withName("SOURCE_SG_POSTGRES_CONFIG")
            .withValue(Optional.of(cluster.getSpec())
                .map(StackGresShardedClusterSpec::getCoordinator)
                .map(StackGresShardedClusterCoordinator::getConfigurationsForCoordinator)
                .map(StackGresClusterConfigurations::getSgPostgresConfig)
                .orElse(""))
            .build(),
            new EnvVarBuilder()
            .withName("BACKUP_PATHS")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getBackupPaths)
                .map(backupPaths -> String.join(" ", backupPaths))
                .orElse(""))
            .build(),
            new EnvVarBuilder()
            .withName("LINK")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getLink)
                .map(String::valueOf)
                .orElse("false"))
            .build(),
            new EnvVarBuilder()
            .withName("CLONE")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getClone)
                .map(String::valueOf)
                .orElse("false"))
            .build(),
            new EnvVarBuilder()
            .withName("CHECK")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getCheck)
                .map(String::valueOf)
                .orElse("false"))
            .build(),
            new EnvVarBuilder()
            .withName("POSTGRES_EXTENSIONS_JSON")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getPostgresExtensions)
                .filter(extensions -> !extensions.isEmpty())
                .map(extensions -> jsonMapper.valueToTree(extensions).toString())
                .orElse(""))
            .build(),
            new EnvVarBuilder()
            .withName("TO_INSTALL_POSTGRES_EXTENSIONS_JSON")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getToInstallPostgresExtensions)
                .filter(extensions -> !extensions.isEmpty())
                .map(extensions -> jsonMapper.valueToTree(extensions).toString())
                .orElse(""))
            .build(),
            new EnvVarBuilder()
            .withName("MAX_ERRORS_AFTER_UPGRADE")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getMaxErrorsAfterUpgrade)
                .map(String::valueOf)
                .orElse(""))
            .build(),
            new EnvVarBuilder()
            .withName("MANUAL_ROLLBACK")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getManualRollback)
                .map(String::valueOf)
                .orElse("false"))
            .build(),
            new EnvVarBuilder()
            .withName("MAX_ERRORS_AFTER_CONTINUE_ON_FAILURE")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresShardedDbOpsMajorVersionUpgrade::getMaxErrorsAfterContinueOnFailure)
                .map(String::valueOf)
                .orElse(""))
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_NAMES")
            .withValue(String.join(" ", clusterNames))
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_SG_POSTGRES_CONFIGS")
            .withValue(clusterSgPostgresConfigs)
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
            .withName("SHARDED_CLUSTER_CRD_KIND")
            .withValue(HasMetadata.getKind(StackGresShardedCluster.class))
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_CRD_KIND")
            .withValue(HasMetadata.getKind(StackGresCluster.class))
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
            .withValue(StackGresKeys.POSTGRES_VERSION_KEY)
            .build(),
            new EnvVarBuilder()
            .withName("LOCK_DURATION")
            .withValue(OperatorProperty.LOCK_DURATION.getString())
            .build(),
            new EnvVarBuilder()
            .withName("LOCK_SLEEP")
            .withValue(OperatorProperty.LOCK_POLL_INTERVAL.getString())
            .build())
        .build();
  }

  @Override
  protected String getRunImage(StackGresShardedDbOpsContext context) {
    return kubectl.getImageName(
        context.getShardedCluster(), context.getFoundCoordinator().orElse(null));
  }

  @Override
  protected ShardedClusterPath getRunScript() {
    return ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_MAJOR_VERSION_UPGRADE_SH_PATH;
  }

  @Override
  protected boolean isExclusiveOp() {
    return true;
  }

}
