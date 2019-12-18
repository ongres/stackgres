/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.batch.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobTemplateSpecBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.controller.ResourceGeneratorContext;
import io.stackgres.operator.customresource.sgbackup.BackupPhase;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDefinition;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.patroni.PatroniRole;
import io.stackgres.operator.resource.ResourceUtil;

import org.jooq.lambda.Unchecked;

public class BackupCronJob {

  /**
   * Create a new CronJob based on the StackGresCluster definition.
   */
  public static List<HasMetadata> create(
      ResourceGeneratorContext<StackGresClusterContext> context) {
    StackGresClusterContext clusterContext = context.getContext();
    String namespace = clusterContext.getCluster().getMetadata().getNamespace();
    String name = clusterContext.getCluster().getMetadata().getName();
    ImmutableMap<String, String> labels = ResourceUtil.defaultLabels(name);
    return ImmutableList.<HasMetadata>builder()
        .addAll(Stream.of(clusterContext.getBackupConfig())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Unchecked.function(backupConfig -> new CronJobBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(name + ClusterStatefulSet.BACKUP_SUFFIX)
                .withLabels(labels)
                .withOwnerReferences(ImmutableList.of(
                    ResourceUtil.getOwnerReference(clusterContext.getCluster())))
                .endMetadata()
                .withNewSpec()
                .withConcurrencyPolicy("Replace")
                .withFailedJobsHistoryLimit(10)
                .withStartingDeadlineSeconds(clusterContext.getBackupConfig()
                    .map(StackGresBackupConfig::getSpec)
                    .map(StackGresBackupConfigSpec::getFullWindow)
                    .orElse(5) * 60L)
                .withSchedule(clusterContext.getBackupConfig()
                    .map(StackGresBackupConfig::getSpec)
                    .map(StackGresBackupConfigSpec::getFullSchedule)
                    .orElse("0 5 * * *"))
                .withJobTemplate(new JobTemplateSpecBuilder()
                    .withNewMetadata()
                    .withNamespace(namespace)
                    .withName(name + ClusterStatefulSet.BACKUP_SUFFIX)
                    .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                    .withNewTemplate()
                    .withNewMetadata()
                    .withNamespace(namespace)
                    .withName(name + ClusterStatefulSet.BACKUP_SUFFIX)
                    .withLabels(ImmutableMap.<String, String>builder()
                        .putAll(labels)
                        .put(ResourceUtil.ROLE_KEY, ResourceUtil.BACKUP_ROLE)
                        .build())
                    .endMetadata()
                    .withNewSpec()
                    .withRestartPolicy("OnFailure")
                    .withServiceAccountName(name + PatroniRole.SUFFIX)
                    .withContainers(new ContainerBuilder()
                        .withName(name + ClusterStatefulSet.BACKUP_SUFFIX)
                        .withImage("bitnami/kubectl:latest")
                        .withEnv(
                            new EnvVarBuilder()
                            .withName("CLUSTER_NAMESPACE")
                            .withValue(namespace)
                            .build(),
                            new EnvVarBuilder()
                            .withName("CLUSTER_NAME")
                            .withValue(name)
                            .build(),
                            new EnvVarBuilder()
                            .withName("CRONJOB_NAME")
                            .withValue(name + ClusterStatefulSet.BACKUP_SUFFIX)
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
                            .withName("BACKUP_PHASE_PENDING")
                            .withValue(BackupPhase.PENDING.label())
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
                            .withValue(ResourceUtil.ROLE_KEY)
                            .build(),
                            new EnvVarBuilder()
                            .withName("PATRONI_PRIMARY_ROLE")
                            .withValue(ResourceUtil.PRIMARY_ROLE)
                            .build(),
                            new EnvVarBuilder()
                            .withName("PATRONI_REPLICA_ROLE")
                            .withValue(ResourceUtil.REPLICA_ROLE)
                            .build(),
                            new EnvVarBuilder()
                            .withName("BACKUP_ROLE")
                            .withValue(ResourceUtil.BACKUP_ROLE)
                            .build(),
                            new EnvVarBuilder()
                            .withName("IS_CRONJOB")
                            .withValue("true")
                            .build(),
                            new EnvVarBuilder()
                            .withName("CLUSTER_LABELS")
                            .withValue(labels
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
                            new EnvVarBuilder().withName("POD_UID")
                            .withValueFrom(
                                new EnvVarSourceBuilder()
                                .withFieldRef(
                                    new ObjectFieldSelectorBuilder()
                                    .withFieldPath("metadata.uid")
                                    .build())
                                .build())
                            .build(),
                            new EnvVarBuilder()
                            .withName("RETAIN")
                            .withValue(clusterContext.getBackupConfig()
                                .map(StackGresBackupConfig::getSpec)
                                .map(StackGresBackupConfigSpec::getRetention)
                                .map(String::valueOf)
                                .orElse("5"))
                            .build(),
                            new EnvVarBuilder()
                            .withName("WINDOW")
                            .withValue(clusterContext.getBackupConfig()
                                .map(StackGresBackupConfig::getSpec)
                                .map(StackGresBackupConfigSpec::getFullWindow)
                                .map(window -> window * 60)
                                .map(String::valueOf)
                                .orElse("3600"))
                            .build())
                        .withCommand("/bin/bash", "-c", Resources
                            .asCharSource(
                                BackupCronJob.class.getResource("/create-backup.sh"),
                                StandardCharsets.UTF_8)
                            .read())
                        .build())
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build())
                .endSpec()
                .build()))
            .iterator())
        .build();
  }

}
