/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

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
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpecBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ClusterPathV1;
import io.stackgres.common.JobUtil;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.LeaseLockUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.VolumeSnapshotUtil;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsSchedulingBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.backup.BackupRetry;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterEnvironmentVariablesFactory;
import io.stackgres.operator.conciliation.factory.cluster.ClusterEnvironmentVariablesFactoryDiscoverer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class BackupCronJob
    implements ResourceGenerator<StackGresClusterContext> {

  private static final Logger BACKUP_LOGGER = LoggerFactory.getLogger("io.stackgres.backup");

  private final StackGresContext context;
  private final LabelFactoryForCluster labelFactory;
  private final ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityFactory;
  private final KubectlUtil kubectl;
  private final ClusterEnvironmentVariablesFactoryDiscoverer clusterEnvVarFactoryDiscoverer;
  private final BackupScriptTemplatesVolumeMounts backupScriptTemplatesVolumeMounts;
  private final BackupTemplatesVolumeFactory backupTemplatesVolumeFactory;

  @Inject
  public BackupCronJob(
      StackGresContext context,
      LabelFactoryForCluster labelFactory,
      ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityFactory,
      KubectlUtil kubectl,
      ClusterEnvironmentVariablesFactoryDiscoverer clusterEnvVarFactoryDiscoverer,
      BackupScriptTemplatesVolumeMounts backupScriptTemplatesVolumeMounts,
      BackupTemplatesVolumeFactory backupTemplatesVolumeFactory) {
    this.context = context;
    this.labelFactory = labelFactory;
    this.podSecurityFactory = podSecurityFactory;
    this.kubectl = kubectl;
    this.clusterEnvVarFactoryDiscoverer = clusterEnvVarFactoryDiscoverer;
    this.backupScriptTemplatesVolumeMounts = backupScriptTemplatesVolumeMounts;
    this.backupTemplatesVolumeFactory = backupTemplatesVolumeFactory;
  }

  public static String backupName(StackGresClusterContext clusterContext) {
    return StackGresUtil.cronJobBackupName(clusterContext.getSource());
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    if (context.getBackupConfiguration().map(BackupConfiguration::cronSchedule).isPresent()) {
      var backupConfig = context.getBackupConfiguration().get();
      return Stream.of(createCronJob(context, backupConfig));
    } else {
      return Stream.of();
    }
  }

  private CronJob createCronJob(StackGresClusterContext context, BackupConfiguration backupConfig) {
    String namespace = context.getSource().getMetadata().getNamespace();
    String name = context.getSource().getMetadata().getName();
    final StackGresCluster cluster = context.getSource();
    Map<String, String> labels = labelFactory.scheduledBackupPodLabels(cluster);
    return new CronJobBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(backupName(context))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withConcurrencyPolicy("Forbid")
        .withFailedJobsHistoryLimit(10)
        .withStartingDeadlineSeconds(5 * 60L)
        .withSchedule(Optional.of(backupConfig)
            .map(BackupConfiguration::cronSchedule)
            .orElse("0 5 * * *"))
        .withJobTemplate(new JobTemplateSpecBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(backupName(context))
            .withLabels(labels)
            .endMetadata()
            .withNewSpec()
            .withBackoffLimit(Optional.of(backupConfig)
                .map(BackupConfiguration::maxRetries)
                .orElse(3))
            .withCompletions(1)
            .withParallelism(1)
            .withNewTemplate()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(backupName(context))
            .withLabels(labels)
            .endMetadata()
            .withNewSpec()
            .withSecurityContext(podSecurityFactory.createResource(context))
            .withRestartPolicy("OnFailure")
            .withServiceAccountName(BackupCronRole.roleName(context))
            .withNodeSelector(Optional.ofNullable(cluster)
                .map(StackGresCluster::getSpec)
                .map(StackGresClusterSpec::getPods)
                .map(StackGresClusterPods::getScheduling)
                .map(StackGresClusterPodsScheduling::getBackup)
                .map(StackGresClusterPodsSchedulingBackup::getNodeSelector)
                .orElse(null))
            .withTolerations(Optional.ofNullable(cluster)
                .map(StackGresCluster::getSpec)
                .map(StackGresClusterSpec::getPods)
                .map(StackGresClusterPods::getScheduling)
                .map(StackGresClusterPodsScheduling::getBackup)
                .map(StackGresClusterPodsSchedulingBackup::getTolerations)
                .map(tolerations -> Seq.seq(tolerations)
                    .map(TolerationBuilder::new)
                    .map(TolerationBuilder::build)
                    .toList())
                .orElse(null))
            .withAffinity(new AffinityBuilder()
                .withNodeAffinity(Optional.of(cluster)
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getPods)
                    .map(StackGresClusterPods::getScheduling)
                    .map(StackGresClusterPodsScheduling::getBackup)
                    .map(StackGresClusterPodsSchedulingBackup::getNodeAffinity)
                    .orElse(null))
                .withPodAffinity(Optional.of(cluster)
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getPods)
                    .map(StackGresClusterPods::getScheduling)
                    .map(StackGresClusterPodsScheduling::getBackup)
                    .map(StackGresClusterPodsSchedulingBackup::getPodAffinity)
                    .orElse(null))
                .withPodAntiAffinity(Optional.of(cluster)
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getPods)
                    .map(StackGresClusterPods::getScheduling)
                    .map(StackGresClusterPodsScheduling::getBackup)
                    .map(StackGresClusterPodsSchedulingBackup::getPodAntiAffinity)
                    .orElse(null))
                .build())
            .withPreemptionPolicy(Optional.ofNullable(cluster.getSpec())
                .map(StackGresClusterSpec::getPods)
                .map(StackGresClusterPods::getScheduling)
                .map(StackGresClusterPodsScheduling::getBackup)
                .map(StackGresClusterPodsSchedulingBackup::getPreemptionPolicy)
                .orElse(null))
            .withPriorityClassName(Optional.ofNullable(cluster.getSpec())
                .map(StackGresClusterSpec::getPods)
                .map(StackGresClusterPods::getScheduling)
                .map(StackGresClusterPodsScheduling::getBackup)
                .map(StackGresClusterPodsSchedulingBackup::getPriorityClassName)
                .orElse(null))
            .withRuntimeClassName(Optional.ofNullable(cluster.getSpec())
                .map(StackGresClusterSpec::getPods)
                .map(StackGresClusterPods::getScheduling)
                .map(StackGresClusterPodsScheduling::getBackup)
                .map(StackGresClusterPodsSchedulingBackup::getRuntimeClassName)
                .orElse(null))
            .withSchedulerName(Optional.ofNullable(cluster.getSpec())
                .map(StackGresClusterSpec::getPods)
                .map(StackGresClusterPods::getScheduling)
                .map(StackGresClusterPodsScheduling::getBackup)
                .map(StackGresClusterPodsSchedulingBackup::getSchedulerName)
                .orElse(null))
            .withContainers(new ContainerBuilder()
                .withName("create-backup")
                .withImage(kubectl.getImageName(cluster))
                .withImagePullPolicy(getDefaultPullPolicy())
                .withEnv(ImmutableList.<EnvVar>builder()
                    .addAll(getClusterEnvVars(context))
                    .add(new EnvVarBuilder()
                        .withName("CLUSTER_NAMESPACE")
                        .withValue(namespace)
                        .build(),
                        new EnvVarBuilder()
                        .withName("CLUSTER_NAME")
                        .withValue(name)
                        .build(),
                        new EnvVarBuilder()
                        .withName("CLUSTER_CRD_NAME")
                        .withValue(CustomResource.getCRDName(StackGresCluster.class))
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_CONFIG_CRD_NAME")
                        .withValue(context.getConfigCrdName())
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_CONFIG")
                        .withValue(context.getBackupConfigurationCustomResourceName()
                            .orElseThrow())
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
                        .withName("USE_VOLUME_SNAPSHOT")
                        .withValue(Optional.of(backupConfig)
                            .map(BackupConfiguration::useVolumeSnapshot)
                            .map(String::valueOf)
                            .orElse("false"))
                        .build(),
                        new EnvVarBuilder()
                        .withName("VOLUME_SNAPSHOT_STORAGE_CLASS")
                        .withValue(Optional.of(backupConfig)
                            .map(BackupConfiguration::volumeSnapshotStorageClass)
                            .map(String::valueOf)
                            .orElse(""))
                        .build(),
                        new EnvVarBuilder()
                        .withName("FAST_VOLUME_SNAPSHOT")
                        .withValue(Optional.of(backupConfig)
                            .map(BackupConfiguration::fastVolumeSnapshot)
                            .map(String::valueOf)
                            .orElse("false"))
                        .build(),
                        new EnvVarBuilder()
                        .withName("RETAIN_WALS_FOR_UNMANAGED_LIFECYCLE")
                        .withValue(Optional.of(backupConfig)
                            .map(BackupConfiguration::retainWalsForUnmanagedLifecycle)
                            .map(String::valueOf)
                            .orElse("false"))
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_TIMEOUT")
                        .withValue(Optional.of(backupConfig)
                            .map(BackupConfiguration::timeout)
                            .map(String::valueOf)
                            .orElse(""))
                        .build(),
                        new EnvVarBuilder()
                        .withName("RECONCILIATION_TIMEOUT")
                        .withValue(Optional.of(backupConfig)
                            .map(BackupConfiguration::reconciliationTimeout)
                            .map(String::valueOf)
                            .orElse("300"))
                        .build(),
                        new EnvVarBuilder()
                        .withName("RETRY_DELAY")
                        .withValue(BackupRetry.getRetryDelay(backupConfig.retryDelay()))
                        .build(),
                        new EnvVarBuilder()
                        .withName("RETRY_LIMIT")
                        .withValue(BackupRetry.getRetryLimit(backupConfig.retryLimit()))
                        .build(),
                        new EnvVarBuilder()
                        .withName("RETRY_MAX_DELAY")
                        .withValue(BackupRetry.getRetryMaxDelay(backupConfig.retryMaxDelay()))
                        .build(),
                        new EnvVarBuilder()
                        .withName("VOLUME_SNAPSHOT_CRD_NAME")
                        .withValue(VolumeSnapshotUtil.VOLUME_SNAPSHOT_CRD_NAME)
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_PHASE_RUNNING")
                        .withValue(BackupStatus.RUNNING.status())
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
                        .withValue(PatroniUtil.getPrimaryRole(this.context, cluster))
                        .build(),
                        new EnvVarBuilder()
                        .withName("PATRONI_REPLICA_ROLE")
                        .withValue(PatroniUtil.REPLICA_ROLE)
                        .build(),
                        new EnvVarBuilder()
                        .withName("SCHEDULED_BACKUP_KEY")
                        .withValue(labelFactory.labelMapper().scheduledBackupKey(cluster))
                        .build(),
                        new EnvVarBuilder()
                        .withName("RIGHT_VALUE")
                        .withValue(StackGresKeys.RIGHT_VALUE)
                        .build(),
                        new EnvVarBuilder()
                        .withName("CLUSTER_LABELS")
                        .withValue(labelFactory.clusterLabels(cluster)
                            .entrySet()
                            .stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining(",")))
                        .build(),
                        new EnvVarBuilder()
                        .withName("PATRONI_CONTAINER_NAME")
                        .withValue(StackGresContainer.PATRONI.getName())
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
                        new EnvVarBuilder().withName("POD_NAME")
                        .withValueFrom(
                            new EnvVarSourceBuilder()
                            .withFieldRef(
                                new ObjectFieldSelectorBuilder()
                                .withFieldPath("metadata.name")
                                .build())
                            .build())
                        .build(),
                        new EnvVarBuilder()
                        .withName("SCHEDULED_BACKUP_JOB_NAME_KEY")
                        .withValue(labelFactory.labelMapper().scheduledBackupJobNameKey(
                            cluster))
                        .build(),
                        new EnvVarBuilder()
                        .withName("SCHEDULED_BACKUP_JOB_NAME")
                        .withValueFrom(
                            new EnvVarSourceBuilder()
                            .withFieldRef(
                                new ObjectFieldSelectorBuilder()
                                .withFieldPath(
                                    "metadata.labels['" + JobUtil.JOB_NAME_KEY + "']")
                                .build())
                            .build())
                        .build(),
                        new EnvVarBuilder()
                        .withName("CLUSTER_BACKUP_NAMESPACES")
                        .withValue(Optional.of(context.getClusterBackupNamespaces()
                            .stream().collect(Collectors.joining(" ")))
                            .filter(Predicates.not(String::isEmpty))
                            .orElse(null))
                        .build(),
                        new EnvVarBuilder()
                        .withName("RETAIN")
                        .withValue(Optional.of(backupConfig)
                            .map(BackupConfiguration::retention)
                            .map(String::valueOf)
                            .orElse("5"))
                        .build(),
                        new EnvVarBuilder()
                        .withName("COMPRESSION")
                        .withValue(
                            Optional.of(backupConfig)
                            .map(BackupConfiguration::compression)
                            .orElse("lz4"))
                        .build(),
                        new EnvVarBuilder()
                        .withName("STORAGE_TEMPLATE_PATH")
                        .withValue(
                            getStorageTemplatePath(context))
                        .build(),
                        new EnvVarBuilder()
                        .withName("HOME")
                        .withValue("/tmp")
                        .build(),
                        new EnvVarBuilder()
                        .withName("LOCK_LEASE_NAMESPACE")
                        .withValue(namespace)
                        .build(),
                        new EnvVarBuilder()
                        .withName("LOCK_LEASE_NAME")
                        .withValue(LeaseLockUtil.leaseNameForCluster(cluster.getMetadata().getUid()))
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
                        .withName("LOCK_GET_RETRIES")
                        .withValue(OperatorProperty.LOCK_GET_RETRIES.getString())
                        .build(),
                        new EnvVarBuilder()
                        .withName("LOCK_GET_RETRY_DELAY")
                        .withValue(OperatorProperty.LOCK_GET_RETRY_DELAY.getString())
                        .build())
                    .build())
                .withCommand("/bin/bash", "-e" + (BACKUP_LOGGER.isTraceEnabled() ? "x" : ""),
                    ClusterPathV1.LOCAL_BIN_CREATE_BACKUP_SH_PATH.path())
                .withVolumeMounts(backupScriptTemplatesVolumeMounts.getVolumeMounts(context))
                .build())
            .withVolumes(backupTemplatesVolumeFactory.buildVolumes(context)
                .map(VolumePair::getVolume)
                .toList())
            .endSpec()
            .endTemplate()
            .endSpec()
            .build())
        .endSpec()
        .build();
  }

  @NotNull
  private String getStorageTemplatePath(StackGresClusterContext context) {
    return context.getObjectStorage().isPresent() ? "spec" : "spec.storage";
  }

  private List<EnvVar> getClusterEnvVars(StackGresClusterContext context) {
    List<EnvVar> clusterEnvVars = new ArrayList<>();

    List<ClusterEnvironmentVariablesFactory> clusterEnvVarFactories =
        clusterEnvVarFactoryDiscoverer.discoverFactories(context);

    clusterEnvVarFactories.forEach(
        envVarFactory -> clusterEnvVars.addAll(envVarFactory.buildEnvironmentVariables(context)));
    return clusterEnvVars;
  }

}
