/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.backup;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.operator.cluster.factory.ClusterStatefulSet;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.common.StackGresBackupContext;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.patroni.factory.PatroniRole;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class Backup implements StackGresClusterResourceStreamFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(Backup.class);

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;

  @Inject
  public Backup(ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables) {
    super();
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
  }

  public static String backupJobName(StackGresBackup backup,
      StackGresClusterContext clusterContext) {
    String name = backup.getMetadata().getName();
    return ResourceUtil.resourceName(
        name + ClusterStatefulSet.BACKUP_SUFFIX);
  }

  public Stream<HasMetadata> streamResources(
      StackGresGeneratorContext context) {
    StackGresClusterContext clusterContext = context.getClusterContext();

    if (!clusterContext.getBackupContext().isPresent()) {
      return Seq.empty();
    }

    return Seq.seq(clusterContext.getBackups())
        .filter(backup -> !(Optional.ofNullable(backup.getStatus())
            .map(StackGresBackupStatus::getProcess)
            .map(StackGresBackupProcess::getStatus)
            .map(phase -> !phase.equals(BackupPhase.RUNNING.label()))
            .orElse(false)
            || Optional.ofNullable(backup.getMetadata())
            .map(ObjectMeta::getOwnerReferences)
            .map(owners -> owners.stream()
                .anyMatch(owner -> owner.getKind().equals("CronJob")))
            .orElse(false)))
        .map(backup -> createBackupJob(backup, clusterContext))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Optional<Job> createBackupJob(StackGresBackup backup,
      StackGresClusterContext context) {
    String namespace = backup.getMetadata().getNamespace();
    String name = backup.getMetadata().getName();
    String cluster = backup.getSpec().getSgCluster();
    ImmutableMap<String, String> labels = context.backupPodLabels();
    return context.getBackupContext()
        .map(StackGresBackupContext::getBackupConfig)
        .map(backupConfig -> new JobBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(backupJobName(backup, context))
            .withLabels(labels)
            .withOwnerReferences(context.ownerReferences())
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
            .withRestartPolicy("OnFailure")
            .withServiceAccountName(PatroniRole.roleName(context))
            .withContainers(new ContainerBuilder()
                .withName("create-backup")
                .withImage("bitnami/kubectl:latest")
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
                        .withValue(cluster + ClusterStatefulSet.BACKUP_SUFFIX)
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_IS_PERMANENT")
                        .withValue(Optional.ofNullable(backup.getSpec()
                            .getManagedLifecycle())
                            .map(String::valueOf)
                            .orElse("true"))
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_CONFIG_CRD_NAME")
                        .withValue(StackGresBackupConfigDefinition.NAME)
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_CONFIG")
                        .withValue(backupConfig.getMetadata().getName())
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_CRD_KIND")
                        .withValue(StackGresBackupDefinition.KIND)
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_CRD_NAME")
                        .withValue(StackGresBackupDefinition.NAME)
                        .build(),
                        new EnvVarBuilder()
                        .withName("BACKUP_CRD_APIVERSION")
                        .withValue(StackGresBackupDefinition.APIVERSION)
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
                        .withValue(StackGresUtil.ROLE_KEY)
                        .build(),
                        new EnvVarBuilder()
                        .withName("PATRONI_PRIMARY_ROLE")
                        .withValue(StackGresUtil.PRIMARY_ROLE)
                        .build(),
                        new EnvVarBuilder()
                        .withName("PATRONI_REPLICA_ROLE")
                        .withValue(StackGresUtil.REPLICA_ROLE)
                        .build(),
                        new EnvVarBuilder()
                        .withName("PATRONI_CLUSTER_LABELS")
                        .withValue(context.patroniClusterLabels()
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
                        .withValue(Optional.ofNullable(backupConfig
                            .getSpec().getBaseBackups().getRetention())
                            .map(String::valueOf)
                            .orElse("5"))
                        .build(),
                        new EnvVarBuilder()
                        .withName("WINDOW")
                        .withValue("3600")
                        .build())
                    .build())
                .withCommand("/bin/bash", "-c" + (LOGGER.isTraceEnabled() ? "x" : ""),
                    Unchecked.supplier(() -> Resources.asCharSource(
                        ClusterStatefulSet.class.getResource("/create-backup.sh"),
                        StandardCharsets.UTF_8)
                    .read()).get())
                .build())
            .endSpec()
            .endTemplate()
            .endSpec()
            .build());
  }

}
