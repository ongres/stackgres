/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractAnnotationDecorator<T> implements Decorator<T> {

  protected abstract @NotNull Map<String, String> getAllResourcesAnnotations(@NotNull T context);

  protected abstract @NotNull Map<String, String> getServiceAnnotations(@NotNull T context);

  protected abstract @NotNull Map<String, String> getPodAnnotations(@NotNull T context);

  @Override
  public HasMetadata decorate(T context, HasMetadata resource) {
    var decoratorMap = getCustomDecorators();

    var decorator = decoratorMap.getOrDefault(resource.getClass(), this::defaultDecorator);
    decorator.accept(context, resource);
    return resource;
  }

  protected void defaultDecorator(@NotNull T context, @NotNull HasMetadata resource) {
    decorateResource(resource, getAllResourcesAnnotations(context));
  }

  protected void decorateResource(@NotNull HasMetadata resource,
      @NotNull Map<String, String> customAnnotations) {
    var metadata = Objects.requireNonNull(resource.getMetadata());

    Map<String, String> resourceAnnotations = Optional.of(metadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    resourceAnnotations.putAll(customAnnotations);

    resource.getMetadata().setAnnotations(resourceAnnotations);
  }

  protected @NotNull Map<Class<?>, BiConsumer<T, HasMetadata>> getCustomDecorators() {
    return Map.of(
        Service.class, this::decorateService,
        Pod.class, this::decoratePod,
        StatefulSet.class, this::decorateSts,
        Job.class, this::decorateJob,
        CronJob.class, this::decorateCronJob,
        io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob.class,
        this::decorateCronJobV1Beta1);
  }

  protected void decorateService(@NotNull T context, @NotNull HasMetadata service) {
    decorateResource(service, getServiceAnnotations(context));
  }

  protected void decoratePod(@NotNull T context, @NotNull HasMetadata pod) {
    decorateResource(pod, getPodAnnotations(context));
  }

  protected void decorateSts(@NotNull T context,
      @NotNull HasMetadata resource) {
    StatefulSet sts = (StatefulSet) resource;

    Map<String, String> jobPodTemplateAnnotations = Optional
        .ofNullable(sts.getSpec())
        .map(StatefulSetSpec::getTemplate)
        .map(PodTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    jobPodTemplateAnnotations.putAll(getPodAnnotations(context));

    Optional.ofNullable(sts.getSpec())
        .map(StatefulSetSpec::getTemplate)
        .ifPresent(podTemplate -> {
          final ObjectMeta podTemplateMetadata = Optional
              .ofNullable(podTemplate.getMetadata())
              .orElse(new ObjectMeta());
          podTemplateMetadata.setAnnotations(jobPodTemplateAnnotations);
          podTemplate.setMetadata(podTemplateMetadata);
        });

    Optional.ofNullable(sts.getSpec())
        .map(StatefulSetSpec::getVolumeClaimTemplates)
        .stream()
        .flatMap(List::stream)
        .forEach(cvt -> decorate(context, cvt));

    decorateResource(sts, getAllResourcesAnnotations(context));
  }

  protected void decorateJob(@NotNull T context,
      @NotNull HasMetadata resource) {
    Job job = (Job) resource;

    Map<String, String> jobPodTemplateAnnotations = Optional
        .ofNullable(job.getSpec())
        .map(JobSpec::getTemplate)
        .map(PodTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    jobPodTemplateAnnotations.putAll(getPodAnnotations(context));

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

  protected void decorateCronJob(@NotNull T context,
      @NotNull HasMetadata resource) {
    CronJob cronJob = (CronJob) resource;

    Map<String, String> cronJobTemplateAnnotations = Optional.ofNullable(cronJob.getSpec())
        .map(CronJobSpec::getJobTemplate)
        .map(JobTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    cronJobTemplateAnnotations.putAll(getAllResourcesAnnotations(context));

    Optional.ofNullable(cronJob.getSpec())
        .map(CronJobSpec::getJobTemplate)
        .ifPresent(template -> {
          final ObjectMeta metadata = Optional
              .ofNullable(template.getMetadata())
              .orElse(new ObjectMeta());
          metadata.setAnnotations(cronJobTemplateAnnotations);
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

  protected void decorateCronJobV1Beta1(@NotNull T context,
      @NotNull HasMetadata resource) {
    io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob cronJob =
        (io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob) resource;

    Map<String, String> cronJobTemplateAnnotations = Optional.ofNullable(cronJob.getSpec())
        .map(io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobSpec::getJobTemplate)
        .map(io.fabric8.kubernetes.api.model.batch.v1beta1.JobTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    cronJobTemplateAnnotations.putAll(getAllResourcesAnnotations(context));

    Optional.ofNullable(cronJob.getSpec())
        .map(io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobSpec::getJobTemplate)
        .ifPresent(template -> {
          final ObjectMeta metadata = Optional
              .ofNullable(template.getMetadata())
              .orElse(new ObjectMeta());
          metadata.setAnnotations(cronJobTemplateAnnotations);
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

}
