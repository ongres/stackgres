/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.batch.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobTemplateSpecBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackupDefinition;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresBackupContext;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.patroni.factory.PatroniRole;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BackupCronJob implements StackGresClusterResourceStreamFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackupCronJob.class);

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;

  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  public BackupCronJob(
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      LabelFactory<StackGresCluster> labelFactory) {
    super();
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
    this.labelFactory = labelFactory;
  }

  /**
   * Create a new CronJob based on the StackGresCluster definition.
   */
  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    StackGresClusterContext clusterContext = context.getClusterContext();
    String namespace = clusterContext.getCluster().getMetadata().getNamespace();
    String name = clusterContext.getCluster().getMetadata().getName();
    final StackGresCluster cluster = clusterContext.getCluster();
    Map<String, String> labels = labelFactory.backupPodLabels(cluster);
    return Seq.of(clusterContext.getBackupContext())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(StackGresBackupContext::getBackupConfig)
        .map(Unchecked.function(backupConfig -> new CronJobBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(ClusterStatefulSet.backupName(clusterContext))
            .withLabels(labels)
            .withOwnerReferences(context.getClusterContext().getOwnerReferences())
            .endMetadata()
            .withNewSpec()
            .withConcurrencyPolicy("Replace")
            .withFailedJobsHistoryLimit(10)
            .withStartingDeadlineSeconds(5 * 60L)
            .withSchedule(Optional.of(backupConfig)
                .map(StackGresBackupConfig::getSpec)
                .map(StackGresBackupConfigSpec::getBaseBackups)
                .map(StackGresBaseBackupConfig::getCronSchedule)
                .orElse("0 5 * * *"))
            .withJobTemplate(new JobTemplateSpecBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(ClusterStatefulSet.backupName(clusterContext))
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(ClusterStatefulSet.backupName(clusterContext))
                .endMetadata()
                .withNewSpec()
                .withRestartPolicy("OnFailure")
                .withServiceAccountName(PatroniRole.roleName(clusterContext))
                .withContainers(new ContainerBuilder()
                    .withName("create-backup")
                    .withImage("bitnami/kubectl:latest")
                    .withEnv(ImmutableList.<EnvVar>builder()
                        .addAll(clusterStatefulSetEnvironmentVariables.listResources(
                            clusterContext))
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
                                .withValue(ClusterStatefulSet.backupName(clusterContext))
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
                                .withName("IS_CRONJOB")
                                .withValue("true")
                                .build(),
                            new EnvVarBuilder()
                                .withName("PATRONI_CLUSTER_LABELS")
                                .withValue(labelFactory.patroniClusterLabels(cluster)
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
                                .withValue(Optional.of(backupConfig)
                                    .map(StackGresBackupConfig::getSpec)
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
                    .withCommand("/bin/bash", "-c" + (LOGGER.isTraceEnabled() ? "x" : ""),
                        Resources.asCharSource(
                            BackupCronJob.class.getResource("/create-backup.sh"),
                            StandardCharsets.UTF_8)
                            .read())
                    .build())
                .endSpec()
                .endTemplate()
                .endSpec()
                .build())
            .endSpec()
            .build()));
  }

}
