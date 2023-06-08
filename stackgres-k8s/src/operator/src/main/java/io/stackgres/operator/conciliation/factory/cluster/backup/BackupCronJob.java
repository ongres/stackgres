/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

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
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.JobUtil;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodSchedulingBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.KubernetesVersionBinder;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterEnvironmentVariablesFactory;
import io.stackgres.operator.conciliation.factory.cluster.ClusterEnvironmentVariablesFactoryDiscoverer;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
@KubernetesVersionBinder(from = "1.21")
public class BackupCronJob
    implements ResourceGenerator<StackGresClusterContext> {

  private static final Logger BACKUP_LOGGER = LoggerFactory.getLogger("io.stackgres.backup");

  private final
      ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext> clusterEnvVarFactoryDiscoverer;
  private final LabelFactoryForCluster<StackGresCluster> labelFactory;
  private final ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityFactory;
  private final KubectlUtil kubectl;
  private final BackupScriptTemplatesVolumeMounts backupScriptTemplatesVolumeMounts;
  private final BackupTemplatesVolumeFactory backupTemplatesVolumeFactory;

  @Inject
  public BackupCronJob(
      ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext> clusterEnvVarFactoryDiscoverer,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityFactory,
      KubectlUtil kubectl,
      BackupScriptTemplatesVolumeMounts backupScriptTemplatesVolumeMounts,
      BackupTemplatesVolumeFactory backupTemplatesVolumeFactory) {
    super();
    this.clusterEnvVarFactoryDiscoverer = clusterEnvVarFactoryDiscoverer;
    this.labelFactory = labelFactory;
    this.podSecurityFactory = podSecurityFactory;
    this.kubectl = kubectl;
    this.backupScriptTemplatesVolumeMounts = backupScriptTemplatesVolumeMounts;
    this.backupTemplatesVolumeFactory = backupTemplatesVolumeFactory;
  }

  public static String backupName(StackGresClusterContext clusterContext) {
    return StackGresUtil.statefulSetBackupPersistentVolumeName(clusterContext.getSource());
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    if (context.getBackupConfiguration().isPresent()) {
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
            .withBackoffLimit(3)
            .withParallelism(1)
            .withNewTemplate()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(backupName(context))
            .withLabels(labels)
            .endMetadata()
            .withNewSpec()
            .withSecurityContext(podSecurityFactory.createResource(context))
            .withRestartPolicy("Never")
            .withServiceAccountName(BackupCronRole.roleName(context))
            .withNodeSelector(Optional.ofNullable(cluster)
                .map(StackGresCluster::getSpec)
                .map(StackGresClusterSpec::getPod)
                .map(StackGresClusterPod::getScheduling)
                .map(StackGresClusterPodScheduling::getBackup)
                .map(StackGresClusterPodSchedulingBackup::getNodeSelector)
                .orElse(null))
            .withTolerations(Optional.ofNullable(cluster)
                .map(StackGresCluster::getSpec)
                .map(StackGresClusterSpec::getPod)
                .map(StackGresClusterPod::getScheduling)
                .map(StackGresClusterPodScheduling::getBackup)
                .map(StackGresClusterPodSchedulingBackup::getTolerations)
                .map(tolerations -> Seq.seq(tolerations)
                    .map(TolerationBuilder::new)
                    .map(TolerationBuilder::build)
                    .toList())
                .orElse(null))
            .withAffinity(new AffinityBuilder()
                .withNodeAffinity(Optional.of(cluster)
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getPod)
                    .map(StackGresClusterPod::getScheduling)
                    .map(StackGresClusterPodScheduling::getBackup)
                    .map(StackGresClusterPodSchedulingBackup::getNodeAffinity)
                    .orElse(null))
                .withPodAffinity(Optional.of(cluster)
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getPod)
                    .map(StackGresClusterPod::getScheduling)
                    .map(StackGresClusterPodScheduling::getBackup)
                    .map(StackGresClusterPodSchedulingBackup::getPodAffinity)
                    .orElse(null))
                .withPodAntiAffinity(Optional.of(cluster)
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getPod)
                    .map(StackGresClusterPod::getScheduling)
                    .map(StackGresClusterPodScheduling::getBackup)
                    .map(StackGresClusterPodSchedulingBackup::getPodAntiAffinity)
                    .orElse(null))
                .build())
            .withPriorityClassName(Optional.of(cluster)
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getPod)
                    .map(StackGresClusterPod::getScheduling)
                    .map(StackGresClusterPodScheduling::getBackup)
                    .map(StackGresClusterPodSchedulingBackup::getPriorityClassName)
                    .orElse(null))
            .withContainers(new ContainerBuilder()
                .withName("create-backup")
                .withImage(kubectl.getImageName(cluster))
                .withImagePullPolicy("IfNotPresent")
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
                            .withName("CRONJOB_NAME")
                            .withValue(backupName(context))
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
                            .withValue(PatroniUtil.PRIMARY_ROLE)
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
                            .withValue(StackGresContext.RIGHT_VALUE)
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
                            .withName("WINDOW")
                            .withValue("3600")
                            .build(),
                        new EnvVarBuilder()
                            .withName("HOME")
                            .withValue("/tmp")
                            .build())
                    .build())
                .withCommand("/bin/bash", "-e" + (BACKUP_LOGGER.isTraceEnabled() ? "x" : ""),
                    ClusterStatefulSetPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH.path())
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
    return context.getObjectStorageConfig().isPresent() ? "spec" : "spec.storage";
  }

  private List<EnvVar> getClusterEnvVars(StackGresClusterContext context) {
    List<EnvVar> clusterEnvVars = new ArrayList<>();

    List<ClusterEnvironmentVariablesFactory<ClusterContext>> clusterEnvVarFactories =
        clusterEnvVarFactoryDiscoverer.discoverFactories(context);

    clusterEnvVarFactories.forEach(
        envVarFactory -> clusterEnvVars.addAll(envVarFactory.buildEnvironmentVariables(context)));
    return clusterEnvVars;
  }

}
