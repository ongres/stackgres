/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedbackup;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;
import static io.stackgres.common.StackGresShardedClusterUtil.getShardClusterName;
import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.ShardedClusterPath;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodSchedulingBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupProcess;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupSpec;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForShardedBackup;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.shardedcluster.ShardedClusterEnvironmentVariablesFactory;
import io.stackgres.operator.conciliation.factory.shardedcluster.ShardedClusterEnvironmentVariablesFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.shardedcluster.backup.ShardedBackupCronRole;
import io.stackgres.operator.conciliation.shardedbackup.ShardedBackupConfiguration;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class ShardedBackupJob
    implements ResourceGenerator<StackGresShardedBackupContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger("io.stackgres.backup");

  private final LabelFactoryForShardedBackup labelFactory;
  private final LabelFactoryForCluster<StackGresCluster> clusterLabelFactory;
  private final ResourceFactory<StackGresShardedBackupContext, PodSecurityContext>
      podSecurityFactory;
  private final KubectlUtil kubectl;
  private final ShardedClusterEnvironmentVariablesFactoryDiscoverer clusterEnvVarFactoryDiscoverer;
  private final ShardedBackupScriptTemplatesVolumeMounts backupScriptTemplatesVolumeMounts;
  private final ShardedBackupTemplatesVolumeFactory backupTemplatesVolumeFactory;

  @Inject
  public ShardedBackupJob(
      LabelFactoryForShardedBackup labelFactory,
      LabelFactoryForCluster<StackGresCluster> clusterLabelFactory,
      ResourceFactory<StackGresShardedBackupContext, PodSecurityContext> podSecurityFactory,
      KubectlUtil kubectl,
      ShardedClusterEnvironmentVariablesFactoryDiscoverer clusterEnvVarFactoryDiscoverer,
      ShardedBackupScriptTemplatesVolumeMounts backupScriptTemplatesVolumeMounts,
      ShardedBackupTemplatesVolumeFactory backupTemplatesVolumeFactory) {
    super();
    this.labelFactory = labelFactory;
    this.clusterLabelFactory = clusterLabelFactory;
    this.podSecurityFactory = podSecurityFactory;
    this.kubectl = kubectl;
    this.clusterEnvVarFactoryDiscoverer = clusterEnvVarFactoryDiscoverer;
    this.backupScriptTemplatesVolumeMounts = backupScriptTemplatesVolumeMounts;
    this.backupTemplatesVolumeFactory = backupTemplatesVolumeFactory;
  }

  public static String backupJobName(StackGresShardedBackup backup) {
    String name = backup.getMetadata().getName();
    return ResourceUtil.resourceName(
        name + StackGresUtil.BACKUP_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedBackupContext context) {
    if (skipBackupJobCreation(context)) {
      return Stream.of();
    }
    return Stream.of(createBackupJob(context));
  }

  public static boolean skipBackupJobCreation(StackGresShardedBackupContext context) {
    return isBackupCopy(context)
        || isScheduledBackupJob(context)
        || isBackupJobFinished(context)
        || (!isBackupJobFinished(context)
            && isBackupConfigNotConfigured(context));
  }

  private static boolean isBackupCopy(StackGresShardedBackupContext context) {
    return StackGresUtil.isRelativeIdNotInSameNamespace(
        context.getSource().getSpec().getSgShardedCluster());
  }

  private static boolean isBackupConfigNotConfigured(StackGresShardedBackupContext context) {
    return context.getObjectStorage().isEmpty();
  }

  private static boolean isBackupJobFinished(StackGresShardedBackupContext context) {
    return Optional.ofNullable(context.getSource().getStatus())
        .map(StackGresShardedBackupStatus::getProcess)
        .map(StackGresShardedBackupProcess::getStatus)
        .filter(status -> status.equals(ShardedBackupStatus.COMPLETED.status())
            || status.equals(ShardedBackupStatus.FAILED.status()))
        .isPresent();
  }

  private static boolean isScheduledBackupJob(StackGresShardedBackupContext context) {
    return Optional.ofNullable(context.getSource().getMetadata().getAnnotations())
        .stream()
        .flatMap(Seq::seq)
        .anyMatch(Tuple.tuple(
            StackGresContext.SCHEDULED_SHARDED_BACKUP_KEY,
            StackGresContext.RIGHT_VALUE)::equals)
        || Optional.ofNullable(context.getSource().getMetadata().getAnnotations())
        .stream()
        .flatMap(Seq::seq)
        .anyMatch(Tuple.tuple(
            StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.SCHEDULED_SHARDED_BACKUP_KEY,
            StackGresContext.RIGHT_VALUE)::equals);
  }

  private Job createBackupJob(StackGresShardedBackupContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();

    StackGresShardedBackup backup = context.getSource();
    String namespace = backup.getMetadata().getNamespace();
    String name = backup.getMetadata().getName();
    String uid = backup.getMetadata().getUid();
    String clusterName = backup.getSpec().getSgShardedCluster();
    var backupConfig = context.getBackupConfiguration();

    Map<String, String> labels = labelFactory.backupPodLabels(context.getSource());
    return new JobBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(backupJobName(backup))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withBackoffLimit(Optional.of(backup)
            .map(StackGresShardedBackup::getSpec)
            .map(StackGresShardedBackupSpec::getMaxRetries)
            .orElse(3))
        .withParallelism(1)
        .withNewTemplate()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(backupJobName(backup))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withSecurityContext(podSecurityFactory.createResource(context))
        .withRestartPolicy("Never")
        .withServiceAccountName(ShardedBackupCronRole.roleName(cluster))
        .withNodeSelector(Optional.ofNullable(cluster)
            .map(StackGresShardedCluster::getSpec)
            .map(StackGresShardedClusterSpec::getCoordinator)
            .map(StackGresClusterSpec::getPods)
            .map(StackGresClusterPods::getScheduling)
            .map(StackGresClusterPodScheduling::getBackup)
            .map(StackGresClusterPodSchedulingBackup::getNodeSelector)
            .orElse(null))
        .withTolerations(Optional.ofNullable(cluster)
            .map(StackGresShardedCluster::getSpec)
            .map(StackGresShardedClusterSpec::getCoordinator)
            .map(StackGresClusterSpec::getPods)
            .map(StackGresClusterPods::getScheduling)
            .map(StackGresClusterPodScheduling::getBackup)
            .map(StackGresClusterPodSchedulingBackup::getTolerations)
            .map(tolerations -> Seq.seq(tolerations)
                .map(TolerationBuilder::new)
                .map(TolerationBuilder::build)
                .toList())
            .orElse(null))
        .withAffinity(new AffinityBuilder()
            .withNodeAffinity(Optional.of(cluster)
                .map(StackGresShardedCluster::getSpec)
                .map(StackGresShardedClusterSpec::getCoordinator)
                .map(StackGresClusterSpec::getPods)
                .map(StackGresClusterPods::getScheduling)
                .map(StackGresClusterPodScheduling::getBackup)
                .map(StackGresClusterPodSchedulingBackup::getNodeAffinity)
                .orElse(null))
            .withPodAffinity(Optional.of(cluster)
                .map(StackGresShardedCluster::getSpec)
                .map(StackGresShardedClusterSpec::getCoordinator)
                .map(StackGresClusterSpec::getPods)
                .map(StackGresClusterPods::getScheduling)
                .map(StackGresClusterPodScheduling::getBackup)
                .map(StackGresClusterPodSchedulingBackup::getPodAffinity)
                .orElse(null))
            .withPodAntiAffinity(Optional.of(cluster)
                .map(StackGresShardedCluster::getSpec)
                .map(StackGresShardedClusterSpec::getCoordinator)
                .map(StackGresClusterSpec::getPods)
                .map(StackGresClusterPods::getScheduling)
                .map(StackGresClusterPodScheduling::getBackup)
                .map(StackGresClusterPodSchedulingBackup::getPodAntiAffinity)
                .orElse(null))
            .build())
        .withPriorityClassName(Optional.of(cluster)
            .map(StackGresShardedCluster::getSpec)
            .map(StackGresShardedClusterSpec::getCoordinator)
            .map(StackGresClusterSpec::getPods)
            .map(StackGresClusterPods::getScheduling)
            .map(StackGresClusterPodScheduling::getBackup)
            .map(StackGresClusterPodSchedulingBackup::getPriorityClassName)
            .orElse(null))
        .withContainers(new ContainerBuilder()
            .withName("create-backup")
            .withImage(kubectl.getImageName(cluster))
            .withImagePullPolicy(getDefaultPullPolicy())
            .withEnv(ImmutableList.<EnvVar>builder()
                .addAll(getClusterEnvVars(context))
                .add(
                    new EnvVarBuilder()
                    .withName("CLUSTER_NAMESPACE")
                    .withValue(namespace)
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDING_TYPE")
                    .withValue(cluster.getSpec().getType())
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_BACKUP_NAME")
                    .withValue(name)
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_BACKUP_UID")
                    .withValue(uid)
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_CLUSTER_NAME")
                    .withValue(clusterName)
                    .build(),
                    new EnvVarBuilder()
                    .withName("CLUSTER_NAMES")
                    .withValue(Seq.of(getCoordinatorClusterName(cluster))
                        .append(Seq.range(0, cluster.getSpec().getShards().getClusters())
                            .map(index -> getShardClusterName(cluster, index)))
                        .toString(" "))
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_BACKUP_IS_PERMANENT")
                    .withValue(Optional.ofNullable(backup.getSpec()
                        .getManagedLifecycle())
                        .map(managedLifecycle -> !managedLifecycle)
                        .map(String::valueOf)
                        .orElse("true"))
                    .build(),
                    new EnvVarBuilder()
                    .withName("$SHARDED_BACKUP_TIMEOUT")
                    .withValue(Optional.ofNullable(backup.getSpec()
                        .getTimeout())
                        .map(String::valueOf)
                        .orElse("null"))
                    .build(),
                    new EnvVarBuilder()
                    .withName("$SHARDED_BACKUP_RECONCILIATION_TIMEOUT")
                    .withValue(Optional.ofNullable(backup.getSpec()
                        .getReconciliationTimeout())
                        .map(String::valueOf)
                        .orElse("300"))
                    .build(),
                    new EnvVarBuilder()
                    .withName("$SHARDED_BACKUP_RETAIN_WALS_FOR_UNMANAGED_LIFECYCLE")
                    .withValue(Optional.of(backupConfig)
                        .map(ShardedBackupConfiguration::retainWalsForUnmanagedLifecycle)
                        .map(String::valueOf)
                        .orElse("false"))
                    .build(),
                    new EnvVarBuilder()
                    .withName("CLUSTER_CRD_NAME")
                    .withValue(CustomResource.getCRDName(StackGresCluster.class))
                    .build(),
                    new EnvVarBuilder()
                    .withName("CLUSTER_CRD_KIND")
                    .withValue(HasMetadata.getKind(StackGresCluster.class))
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_BACKUP_CRD_KIND")
                    .withValue(HasMetadata.getKind(StackGresShardedBackup.class))
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_BACKUP_CRD_NAME")
                    .withValue(CustomResource.getCRDName(StackGresShardedBackup.class))
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_BACKUP_CRD_APIVERSION")
                    .withValue(HasMetadata.getApiVersion(StackGresShardedBackup.class))
                    .build(),
                    new EnvVarBuilder()
                    .withName("BACKUP_CRD_KIND")
                    .withValue(HasMetadata.getKind(StackGresBackup.class))
                    .build(),
                    new EnvVarBuilder()
                    .withName("BACKUP_CRD_NAME")
                    .withValue(CustomResource.getCRDName(StackGresBackup.class))
                    .build(),
                    new EnvVarBuilder()
                    .withName("BACKUP_CRD_APIVERSION")
                    .withValue(HasMetadata.getApiVersion(StackGresBackup.class))
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_BACKUP_PHASE_RUNNING")
                    .withValue(ShardedBackupStatus.RUNNING.status())
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_BACKUP_PHASE_COMPLETED")
                    .withValue(ShardedBackupStatus.COMPLETED.status())
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_BACKUP_PHASE_FAILED")
                    .withValue(ShardedBackupStatus.FAILED.status())
                    .build(),
                    new EnvVarBuilder()
                    .withName("BACKUP_PHASE_COMPLETED")
                    .withValue(BackupStatus.COMPLETED.status())
                    .build(),
                    new EnvVarBuilder()
                    .withName("BACKUP_PHASE_FAILED")
                    .withValue(BackupStatus.FAILED.status())
                    .build(),
                    new EnvVarBuilder()
                    .withName("PATRONI_ROLE_KEY")
                    .withValue(PatroniUtil.ROLE_KEY)
                    .build(),
                    new EnvVarBuilder()
                    .withName("PATRONI_PRIMARY_ROLE")
                    .withValue(PatroniUtil.PRIMARY_ROLE)
                    .build(),
                    new EnvVarBuilder()
                    .withName("PATRONI_REPLICA_ROLE")
                    .withValue(PatroniUtil.REPLICA_ROLE)
                    .build(),
                    new EnvVarBuilder()
                    .withName("PATRONI_CONTAINER_NAME")
                    .withValue(StackGresContainer.PATRONI.getName())
                    .build(),
                    new EnvVarBuilder()
                    .withName("RIGHT_VALUE")
                    .withValue(StackGresContext.RIGHT_VALUE)
                    .build(),
                    new EnvVarBuilder()
                    .withName("SHARDED_CLUSTER_DATABASE")
                    .withValue(cluster.getSpec().getDatabase())
                    .build(),
                    new EnvVarBuilder()
                    .withName("COORDINATOR_CLUSTER_LABELS")
                    .withValue(clusterLabelFactory.clusterLabelsWithoutUid(
                        context.getCoordinatorCluster())
                        .entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(",")))
                    .build(),
                    new EnvVarBuilder()
                    .withName("SERVICE_ACCOUNT")
                    .withValueFrom(
                        new EnvVarSourceBuilder()
                        .withFieldRef(
                            new ObjectFieldSelectorBuilder()
                            .withFieldPath("spec.serviceAccountName")
                            .build())
                        .build())
                    .build(),
                    new EnvVarBuilder()
                    .withName("POD_NAME")
                    .withValueFrom(
                        new EnvVarSourceBuilder()
                        .withFieldRef(
                            new ObjectFieldSelectorBuilder()
                            .withFieldPath("metadata.name")
                            .build())
                        .build())
                    .build(),
                    new EnvVarBuilder()
                    .withName("CLUSTER_SHARDED_BACKUP_NAMESPACES")
                    .withValue(Optional.of(context.getClusterBackupNamespaces()
                        .stream().collect(Collectors.joining(" ")))
                        .filter(Predicates.not(String::isEmpty))
                        .orElse(null))
                    .build(),
                    new EnvVarBuilder()
                    .withName("HOME")
                    .withValue("/tmp")
                    .build(),
                    new EnvVarBuilder()
                    .withName("LOCK_RESOURCE_NAME")
                    .withValue(clusterName)
                    .build(),
                    new EnvVarBuilder()
                    .withName("LOCK_RESOURCE")
                    .withValue(HasMetadata.getFullResourceName(StackGresShardedCluster.class))
                    .build(),
                    new EnvVarBuilder()
                    .withName("LOCK_DURATION")
                    .withValue(OperatorProperty.LOCK_DURATION.getString())
                    .build(),
                    new EnvVarBuilder()
                    .withName("LOCK_POLL_INTERVAL")
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
                .build())
            .withCommand("/bin/bash", "-e" + (LOGGER.isTraceEnabled() ? "x" : ""),
                ShardedClusterPath.LOCAL_BIN_CREATE_SHARDED_BACKUP_SH_PATH.path())
            .withVolumeMounts(backupScriptTemplatesVolumeMounts.getVolumeMounts(context))
            .build())
        .withVolumes(backupTemplatesVolumeFactory.buildVolumes(context)
            .map(VolumePair::getVolume)
            .toList())
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private List<EnvVar> getClusterEnvVars(StackGresShardedBackupContext context) {
    List<EnvVar> clusterEnvVars = new ArrayList<>();

    List<ShardedClusterEnvironmentVariablesFactory> clusterEnvVarFactories =
        clusterEnvVarFactoryDiscoverer.discoverFactories(context);

    clusterEnvVarFactories.forEach(
        envVarFactory -> clusterEnvVars.addAll(envVarFactory.buildEnvironmentVariables(context)));
    return clusterEnvVars;
  }

}

