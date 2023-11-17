/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedbackup;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.AnnotationDecorator;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class ShardedBackupAnnotationDecorator
    extends AnnotationDecorator<StackGresShardedBackupContext> {

  @Override
  protected @NotNull Map<String, String> getAllResourcesAnnotations(
      @NotNull StackGresShardedBackupContext context) {
    return Optional.ofNullable(context.getShardedCluster().getSpec())
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getAllResources)
        .orElse(Map.of());
  }

  @Override
  protected @NotNull Map<String, String> getServiceAnnotations(
      @NotNull StackGresShardedBackupContext context) {
    Map<String, String> servicesSpecificAnnotations =
        Optional.ofNullable(context.getShardedCluster().getSpec())
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getServices)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(servicesSpecificAnnotations)
        .build();
  }

  protected @NotNull Map<String, String> getPrimaryServiceAnnotations(
      @NotNull StackGresShardedBackupContext context) {
    Map<String, String> primaryServiceAnnotations =
        Optional.ofNullable(context.getShardedCluster().getSpec())
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getPrimaryService)
        .orElse(ImmutableMap.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(context))
        .putAll(primaryServiceAnnotations)
        .build();
  }

  protected @NotNull Map<String, String> getReplicaServiceAnnotations(
      @NotNull StackGresShardedBackupContext context) {
    Map<String, String> replicaServiceAnnotations =
        Optional.ofNullable(context.getShardedCluster().getSpec())
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getReplicasService)
        .orElse(ImmutableMap.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(context))
        .putAll(replicaServiceAnnotations)
        .build();
  }

  @Override
  protected void decorateService(@NotNull StackGresShardedBackupContext context,
      @NotNull HasMetadata service) {
    Map<String, String> customServiceAnnotations;

    final String serviceName = service.getMetadata().getName();
    if (serviceName.endsWith(PatroniUtil.DEPRECATED_READ_WRITE_SERVICE)) {
      customServiceAnnotations = getPrimaryServiceAnnotations(context);
    } else if (serviceName.endsWith(PatroniUtil.READ_ONLY_SERVICE)) {
      customServiceAnnotations = getReplicaServiceAnnotations(context);
    } else {
      customServiceAnnotations = getServiceAnnotations(context);
    }

    decorateResource(service, customServiceAnnotations);
  }

  @Override
  protected @NotNull Map<String, String> getPodAnnotations(
      @NotNull StackGresShardedBackupContext context) {
    Map<String, String> podSpecificAnnotations =
        Optional.ofNullable(context.getShardedCluster().getSpec())
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getClusterPods)
        .orElse(Map.of());

    final Map<String, String> clusterAnnotations =
        context.getSource().getMetadata().getAnnotations();

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(podSpecificAnnotations)
        .put(StackGresContext.VERSION_KEY, clusterAnnotations.get(StackGresContext.VERSION_KEY))
        .build();
  }

  protected @NotNull Map<String, String> getJobAnnotations(
      @NotNull StackGresShardedBackupContext context) {
    Map<String, String> podSpecificAnnotations =
        Optional.ofNullable(context.getShardedCluster().getSpec())
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getClusterPods)
        .orElse(Map.of());
    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(podSpecificAnnotations)
        .put(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString())
        .build();
  }

  protected void decorateJob(@NotNull StackGresShardedBackupContext context,
      @NotNull HasMetadata resource) {
    Job job = (Job) resource;

    Map<String, String> jobPodTemplateAnnotations = Optional
        .ofNullable(job.getSpec())
        .map(JobSpec::getTemplate)
        .map(PodTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    jobPodTemplateAnnotations.putAll(getJobAnnotations(context));

    Optional.ofNullable(job.getSpec())
        .map(JobSpec::getTemplate)
        .ifPresent(podTemplate -> {
          final ObjectMeta podTemplateMetadata = Optional
              .ofNullable(podTemplate.getMetadata())
              .orElse(new ObjectMeta());
          podTemplateMetadata.setAnnotations(jobPodTemplateAnnotations);
          podTemplate.setMetadata(podTemplateMetadata);
        });
    decorateResource(job, getAllResourcesAnnotations(context));
  }

  protected void decorateCronJob(@NotNull StackGresShardedBackupContext context,
      @NotNull HasMetadata resource) {
    CronJob cronJob = (CronJob) resource;

    Map<String, String> jobTemplateAnnotations = Optional.ofNullable(cronJob.getSpec())
        .map(CronJobSpec::getJobTemplate)
        .map(JobTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());
    jobTemplateAnnotations.putAll(getAllResourcesAnnotations(context));

    Optional.ofNullable(cronJob.getSpec())
        .map(CronJobSpec::getJobTemplate)
        .ifPresent(template -> {
          final ObjectMeta metadata = Optional
              .ofNullable(template.getMetadata())
              .orElse(new ObjectMeta());
          metadata.setAnnotations(jobTemplateAnnotations);
          template.setMetadata(metadata);

          Map<String, String> cronJobPodTemplateAnnotations = Optional
              .ofNullable(cronJob.getSpec())
              .map(CronJobSpec::getJobTemplate)
              .map(JobTemplateSpec::getSpec)
              .map(JobSpec::getTemplate)
              .map(PodTemplateSpec::getMetadata)
              .map(ObjectMeta::getAnnotations)
              .orElse(new HashMap<>());

          cronJobPodTemplateAnnotations.putAll(getPodAnnotations(context));

          Optional.ofNullable(template.getSpec())
              .map(JobSpec::getTemplate)
              .ifPresent(podTemplate -> {
                final ObjectMeta podTemplateMetadata = Optional
                    .ofNullable(podTemplate.getMetadata())
                    .orElse(new ObjectMeta());
                podTemplateMetadata.setAnnotations(cronJobPodTemplateAnnotations);
                podTemplate.setMetadata(podTemplateMetadata);
              });
        });
    decorateResource(cronJob, getAllResourcesAnnotations(context));
  }

  protected void decorateCronJobV1Beta1(@NotNull StackGresShardedBackupContext context,
      @NotNull HasMetadata resource) {
    io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob cronJob =
        (io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob) resource;

    Map<String, String> jobTemplateAnnotations = Optional.ofNullable(cronJob.getSpec())
        .map(io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobSpec::getJobTemplate)
        .map(io.fabric8.kubernetes.api.model.batch.v1beta1.JobTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());
    jobTemplateAnnotations.putAll(getAllResourcesAnnotations(context));

    Optional.ofNullable(cronJob.getSpec())
        .map(io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobSpec::getJobTemplate)
        .ifPresent(template -> {
          final ObjectMeta metadata = Optional
              .ofNullable(template.getMetadata())
              .orElse(new ObjectMeta());
          metadata.setAnnotations(jobTemplateAnnotations);
          template.setMetadata(metadata);

          Map<String, String> cronJobPodTemplateAnnotations = Optional
              .ofNullable(cronJob.getSpec())
              .map(io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobSpec::getJobTemplate)
              .map(io.fabric8.kubernetes.api.model.batch.v1beta1.JobTemplateSpec::getSpec)
              .map(JobSpec::getTemplate)
              .map(PodTemplateSpec::getMetadata)
              .map(ObjectMeta::getAnnotations)
              .orElse(new HashMap<>());

          cronJobPodTemplateAnnotations.putAll(getPodAnnotations(context));

          Optional.ofNullable(template.getSpec())
              .map(JobSpec::getTemplate)
              .ifPresent(podTemplate -> {
                final ObjectMeta podTemplateMetadata = Optional
                    .ofNullable(podTemplate.getMetadata())
                    .orElse(new ObjectMeta());
                podTemplateMetadata.setAnnotations(cronJobPodTemplateAnnotations);
                podTemplate.setMetadata(podTemplateMetadata);
              });
        });
    decorateResource(cronJob, getAllResourcesAnnotations(context));
  }

  @Override
  protected @NotNull Map<Class<?>, BiConsumer<StackGresShardedBackupContext, HasMetadata>>
      getCustomDecorators() {
    return ImmutableMap.<Class<?>,
            BiConsumer<StackGresShardedBackupContext, HasMetadata>>builder()
        .putAll(super.getCustomDecorators())
        .put(Job.class, this::decorateJob)
        .put(CronJob.class, this::decorateCronJob)
        .put(io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob.class,
            this::decorateCronJobV1Beta1)
        .build();
  }
}
