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

import javax.annotation.Nonnull;

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

public abstract class AbstractAnnotationDecorator<T> implements Decorator<T> {

  protected abstract @Nonnull Map<String, String> getAllResourcesAnnotations(@Nonnull T context);

  protected abstract @Nonnull Map<String, String> getServiceAnnotations(@Nonnull T context);

  protected abstract @Nonnull Map<String, String> getPodAnnotations(@Nonnull T context);

  @Override
  public HasMetadata decorate(T context, HasMetadata resource) {
    var decoratorMap = getCustomDecorators();

    var decorator = decoratorMap.getOrDefault(resource.getClass(), this::defaultDecorator);
    decorator.accept(context, resource);
    return resource;
  }

  protected void defaultDecorator(@Nonnull T context, @Nonnull HasMetadata resource) {
    decorateResource(resource, getAllResourcesAnnotations(context));
  }

  protected void decorateResource(@Nonnull HasMetadata resource,
      @Nonnull Map<String, String> customAnnotations) {
    var metadata = Objects.requireNonNull(resource.getMetadata());

    Map<String, String> resourceAnnotations = Optional.of(metadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    resourceAnnotations.putAll(customAnnotations);

    resource.getMetadata().setAnnotations(resourceAnnotations);
  }

  protected @Nonnull Map<Class<?>, BiConsumer<T, HasMetadata>> getCustomDecorators() {
    return Map.of(
        Service.class, this::decorateService,
        Pod.class, this::decoratePod,
        StatefulSet.class, this::decorateSts,
        Job.class, this::decorateJob,
        CronJob.class, this::decorateCronJob,
        io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob.class,
        this::decorateCronJobV1Beta1);
  }

  protected void decorateService(@Nonnull T context, @Nonnull HasMetadata service) {
    decorateResource(service, getServiceAnnotations(context));
  }

  protected void decoratePod(@Nonnull T context, @Nonnull HasMetadata pod) {
    decorateResource(pod, getPodAnnotations(context));
  }

  protected void decorateSts(@Nonnull T context,
      @Nonnull HasMetadata resource) {
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

  protected void decorateJob(@Nonnull T context,
      @Nonnull HasMetadata resource) {
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

  protected void decorateCronJob(@Nonnull T context,
      @Nonnull HasMetadata resource) {
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

  protected void decorateCronJobV1Beta1(@Nonnull T context,
      @Nonnull HasMetadata resource) {
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
