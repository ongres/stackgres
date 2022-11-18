/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.inject.Singleton;

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
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.AnnotationDecorator;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class DbOpsAnnotationDecorator extends AnnotationDecorator<StackGresDbOpsContext> {

  @Override
  protected @NotNull Map<String, String> getAllResourcesAnnotations(
      @NotNull StackGresDbOpsContext context) {
    return Optional.ofNullable(context.getCluster().getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getAllResources)
        .orElse(Map.of());
  }

  @Override
  protected @NotNull Map<String, String> getServiceAnnotations(
      @NotNull StackGresDbOpsContext context) {
    Map<String, String> servicesSpecificAnnotations =
        Optional.ofNullable(context.getCluster().getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getServices)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(servicesSpecificAnnotations)
        .build();
  }

  protected @NotNull Map<String, String> getPrimaryServiceAnnotations(
      @NotNull StackGresDbOpsContext context) {
    Map<String, String> primaryServiceAnnotations =
        Optional.ofNullable(context.getCluster().getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getPrimaryService)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(context))
        .putAll(primaryServiceAnnotations)
        .build();
  }

  protected @NotNull Map<String, String> getReplicaServiceAnnotations(
      @NotNull StackGresDbOpsContext context) {
    Map<String, String> replicaServiceAnnotations =
        Optional.ofNullable(context.getCluster().getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getReplicasService)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(context))
        .putAll(replicaServiceAnnotations)
        .build();
  }

  @Override
  protected void decorateService(@NotNull StackGresDbOpsContext context,
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
      @NotNull StackGresDbOpsContext context) {
    Map<String, String> podSpecificAnnotations =
        Optional.ofNullable(context.getCluster().getSpec())
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
      @NotNull StackGresDbOpsContext context) {
    Map<String, String> podSpecificAnnotations =
        Optional.ofNullable(context.getCluster().getSpec())
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

  protected void decorateJob(@NotNull StackGresDbOpsContext context,
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

  protected void decorateCronJob(@NotNull StackGresDbOpsContext context,
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

  @Override
  protected @NotNull Map<String, BiConsumer<StackGresDbOpsContext, HasMetadata>>
      getCustomDecorators() {
    return ImmutableMap.<String, BiConsumer<StackGresDbOpsContext, HasMetadata>>builder()
        .putAll(super.getCustomDecorators())
        .put("Job", this::decorateJob)
        .put("CronJob", this::decorateCronJob)
        .build();
  }
}
