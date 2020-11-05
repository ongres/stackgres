/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.backup;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.StackGresBackupContext;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresPodSecurityContext;
import io.stackgres.operator.patroni.factory.PatroniRole;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BackupJob implements StackGresClusterResourceStreamFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackupJob.class);

  private final StackGresPodSecurityContext clusterPodSecurityContext;
  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;

  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  public BackupJob(StackGresPodSecurityContext clusterPodSecurityContext,
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      LabelFactory<StackGresCluster> labelFactory) {
    super();
    this.clusterPodSecurityContext = clusterPodSecurityContext;
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
    this.labelFactory = labelFactory;
  }

  public static String backupJobName(StackGresBackup backup,
      StackGresClusterContext clusterContext) {
    String name = backup.getMetadata().getName();
    return ResourceUtil.resourceName(
        name + StackGresUtil.BACKUP_SUFFIX);
  }

  public Stream<HasMetadata> streamResources(StackGresClusterContext context) {
    if (!context.getBackupContext().isPresent()) {
      return Seq.empty();
    }

    return Seq.seq(context.getBackups())
        .filter(backup -> !Optional.ofNullable(backup.getStatus())
            .map(StackGresBackupStatus::getProcess)
            .map(StackGresBackupProcess::getStatus)
            .map(status -> status.equals(BackupPhase.COMPLETED.label()))
            .orElse(false)
            && !Seq.seq(backup.getMetadata().getAnnotations())
            .anyMatch(Tuple.tuple(
                StackGresContext.SCHEDULED_BACKUP_KEY, StackGresContext.RIGHT_VALUE)::equals))
        .map(backup -> createBackupJob(backup, context))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Optional<Job> createBackupJob(StackGresBackup backup,
      StackGresClusterContext context) {
    String namespace = backup.getMetadata().getNamespace();
    String name = backup.getMetadata().getName();
    String cluster = backup.getSpec().getSgCluster();
    Map<String, String> labels = labelFactory.backupPodLabels(context.getCluster());
    return context.getBackupContext()
        .map(StackGresBackupContext::getBackupConfig)
        .map(backupConfig -> new JobBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(backupJobName(backup, context))
            .withLabels(labels)
            .withOwnerReferences(context.getOwnerReferences())
            .endMetadata()
            .withNewSpec()
            .withBackoffLimit(3)
            .withCompletions(1)
            .withParallelism(1)
            .withNewTemplate()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(backupJobName(backup, context))
            .withLabels(labels)
            .endMetadata()
            .withNewSpec()
            .withSecurityContext(clusterPodSecurityContext.createResource(context))
            .withRestartPolicy("OnFailure")
            .withServiceAccountName(PatroniRole.roleName(context))
            .withContainers(new ContainerBuilder()
                .withName("create-backup")
                .withImage(StackGresContext.KUBECTL_IMAGE)
                .withImagePullPolicy("IfNotPresent")
                .withEnv(ImmutableList.<EnvVar>builder()
                    .addAll(clusterStatefulSetEnvironmentVariables.listResources(context))
                    .add(new EnvVarBuilder()
                        .withName("CLUSTER_NAMESPACE")
                        .withValue(namespace)
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_NAME")
                        .withValue(name)
                        .build(),
                        new EnvVarBuilder()
                        .withName("CLUSTER_NAME")
                        .withValue(cluster)
                        .build(),
                        new EnvVarBuilder()
                        .withName("CRONJOB_NAME")
                        .withValue(cluster + StackGresUtil.BACKUP_SUFFIX)
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_IS_PERMANENT")
                        .withValue(Optional.ofNullable(backup.getSpec()
                            .getManagedLifecycle())
                            .map(managedLifecycle -> !managedLifecycle)
                            .map(String::valueOf)
                            .orElse("true"))
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_CONFIG_CRD_NAME")
                        .withValue(CustomResource.getCRDName(StackGresBackupConfig.class))
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_CONFIG")
                        .withValue(backupConfig.getMetadata().getName())
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
                        .withValue(BackupPhase.RUNNING.label())
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_PHASE_COMPLETED")
                        .withValue(BackupPhase.COMPLETED.label())
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_PHASE_FAILED")
                        .withValue(BackupPhase.FAILED.label())
                        .build(),
                        new EnvVarBuilder()
                        .withName("PATRONI_ROLE_KEY")
                        .withValue(StackGresContext.ROLE_KEY)
                        .build(),
                        new EnvVarBuilder()
                        .withName("PATRONI_PRIMARY_ROLE")
                        .withValue(StackGresContext.PRIMARY_ROLE)
                        .build(),
                        new EnvVarBuilder()
                        .withName("PATRONI_REPLICA_ROLE")
                        .withValue(StackGresContext.REPLICA_ROLE)
                        .build(),
                        new EnvVarBuilder()
                        .withName("PATRONI_CLUSTER_LABELS")
                        .withValue(labelFactory.patroniClusterLabels(context.getCluster())
                            .entrySet()
                            .stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining(",")))
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
                        .withName("RETAIN")
                        .withValue(Optional.of(backupConfig.getSpec())
                            .map(StackGresBackupConfigSpec::getBaseBackups)
                            .map(StackGresBaseBackupConfig::getRetention)
                            .map(String::valueOf)
                            .orElse("5"))
                        .build(),
                        new EnvVarBuilder()
                        .withName("WINDOW")
                        .withValue("3600")
                        .build())
                    .build())
                .withCommand("/bin/bash", "-e" + (LOGGER.isTraceEnabled() ? "x" : ""),
                    ClusterStatefulSetPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH.path())
                .withVolumeMounts(
                    ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context,
                        volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH
                            .filename())
                        .withMountPath(ClusterStatefulSetPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH
                            .path())
                        .withReadOnly(true)),
                    ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context,
                        volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.LOCAL_BIN_SHELL_UTILS_PATH.filename())
                        .withMountPath(ClusterStatefulSetPath.LOCAL_BIN_SHELL_UTILS_PATH.path())
                        .withReadOnly(true)))
                .build())
            .withVolumes(new VolumeBuilder(ClusterStatefulSetVolumeConfig.TEMPLATES.volume(context))
                .editConfigMap()
                .withDefaultMode(0555) // NOPMD
                .endConfigMap()
                .build())
            .endSpec()
            .endTemplate()
            .endSpec()
            .build());
  }

}
