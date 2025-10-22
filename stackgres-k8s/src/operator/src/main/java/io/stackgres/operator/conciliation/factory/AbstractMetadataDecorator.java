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
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;

public abstract class AbstractMetadataDecorator<T> implements Decorator<T> {

  protected abstract Map<String, String> getAllResourcesAnnotations(T context);

  protected abstract Map<String, String> getAllResourcesLabels(T context);

  @Override
  public HasMetadata decorate(T context, HasMetadata resource) {
    var decoratorMap = getCustomDecorators();

    var decorator = decoratorMap.getOrDefault(resource.getClass(), this::defaultDecorator);
    decorator.accept(context, resource);
    return resource;
  }

  protected void defaultDecorator(T context, HasMetadata resource) {
    decorateResourceMetadata(
        resource.getMetadata(),
        context);
  }

  protected void decorateResourceMetadata(
      ObjectMeta resourceMetadata,
      T context) {
    Map<String, String> customAnnotations = getAllResourcesAnnotations(context);
    Map<String, String> customLabels = getAllResourcesLabels(context);

    decorateResourceMetadata(resourceMetadata, customAnnotations, customLabels);
  }

  protected void decorateResourceMetadata(
      ObjectMeta resourceMetadata,
      Map<String, String> customAnnotations,
      Map<String, String> customLabels) {
    var metadata = Objects.requireNonNull(resourceMetadata);

    if (!customAnnotations.isEmpty()) {
      Map<String, String> resourceAnnotations = new HashMap<>();
      resourceAnnotations.putAll(customAnnotations);
      resourceAnnotations.putAll(Optional.of(metadata)
          .map(ObjectMeta::getAnnotations)
          .orElse(new HashMap<>()));
      metadata.setAnnotations(resourceAnnotations);
    }

    if (!customLabels.isEmpty()) {
      Map<String, String> resourceLabels = new HashMap<>();
      resourceLabels.putAll(customLabels);
      resourceLabels.putAll(Optional.of(metadata)
          .map(ObjectMeta::getLabels)
          .orElse(new HashMap<>()));
      metadata.setLabels(resourceLabels);
    }
  }

  protected Map<Class<?>, BiConsumer<T, HasMetadata>> getCustomDecorators() {
    return Map.of(
        StatefulSet.class, this::decorateSts,
        Job.class, this::decorateJob,
        CronJob.class, this::decorateCronJob,
        io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob.class,
        this::decorateCronJobV1Beta1);
  }

  protected void decorateSts(
      T context,
      HasMetadata resource) {
    StatefulSet sts = (StatefulSet) resource;

    Optional
        .ofNullable(sts.getSpec())
        .map(StatefulSetSpec::getTemplate)
        .ifPresent(podTemplate -> {
          final ObjectMeta podTemplateMetadata = Optional
              .ofNullable(podTemplate.getMetadata())
              .orElseGet(ObjectMeta::new);
          podTemplate.setMetadata(podTemplateMetadata);
          decorateResourceMetadata(podTemplateMetadata, context);
        });

    Optional
        .ofNullable(sts.getSpec())
        .map(StatefulSetSpec::getVolumeClaimTemplates)
        .stream()
        .flatMap(List::stream)
        .forEach(pvcTemplate -> {
          final ObjectMeta pvcTemplateMetadata = Optional
              .ofNullable(pvcTemplate.getMetadata())
              .orElseGet(ObjectMeta::new);
          pvcTemplate.setMetadata(pvcTemplateMetadata);
          decorateResourceMetadata(pvcTemplateMetadata, context);
        });

    decorateResourceMetadata(sts.getMetadata(), context);
  }

  protected void decorateJob(
      T context,
      HasMetadata resource) {
    Job job = (Job) resource;

    Optional
        .ofNullable(job.getSpec())
        .map(JobSpec::getTemplate)
        .ifPresent(podTemplate -> {
          final ObjectMeta podTemplateMetadata = Optional
              .ofNullable(podTemplate.getMetadata())
              .orElseGet(ObjectMeta::new);
          podTemplate.setMetadata(podTemplateMetadata);
          decorateResourceMetadata(podTemplateMetadata, context);
        });

    decorateResourceMetadata(job.getMetadata(), context);
  }

  protected void decorateCronJob(
      T context,
      HasMetadata resource) {
    CronJob cronJob = (CronJob) resource;

    Optional
        .ofNullable(cronJob.getSpec())
        .map(CronJobSpec::getJobTemplate)
        .ifPresent(jobTemplate -> {
          final ObjectMeta jobTemplateMetadata = Optional
              .ofNullable(jobTemplate.getMetadata())
              .orElseGet(ObjectMeta::new);
          jobTemplate.setMetadata(jobTemplateMetadata);
          decorateResourceMetadata(jobTemplateMetadata, context);
        });

    Optional
        .ofNullable(cronJob.getSpec())
        .map(CronJobSpec::getJobTemplate)
        .map(JobTemplateSpec::getSpec)
        .map(JobSpec::getTemplate)
        .ifPresent(podTemplate -> {
          final ObjectMeta podTemplateMetadata = Optional
              .ofNullable(podTemplate.getMetadata())
              .orElseGet(ObjectMeta::new);
          podTemplate.setMetadata(podTemplateMetadata);
          decorateResourceMetadata(podTemplateMetadata, context);
        });

    decorateResourceMetadata(cronJob.getMetadata(), context);
  }

  protected void decorateCronJobV1Beta1(
      T context,
      HasMetadata resource) {
    io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob cronJob =
        (io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob) resource;

    Optional
        .ofNullable(cronJob.getSpec())
        .map(io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobSpec::getJobTemplate)
        .ifPresent(jobTemplate -> {
          final ObjectMeta jobTemplateMetadata = Optional
              .ofNullable(jobTemplate.getMetadata())
              .orElseGet(ObjectMeta::new);
          jobTemplate.setMetadata(jobTemplateMetadata);
          decorateResourceMetadata(jobTemplateMetadata, context);
        });

    Optional
        .ofNullable(cronJob.getSpec())
        .map(io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobSpec::getJobTemplate)
        .map(io.fabric8.kubernetes.api.model.batch.v1beta1.JobTemplateSpec::getSpec)
        .map(JobSpec::getTemplate)
        .ifPresent(podTemplate -> {
          final ObjectMeta podTemplateMetadata = Optional
              .ofNullable(podTemplate.getMetadata())
              .orElseGet(ObjectMeta::new);
          podTemplate.setMetadata(podTemplateMetadata);
          decorateResourceMetadata(podTemplateMetadata, context);
        });

    decorateResourceMetadata(cronJob.getMetadata(), context);
  }

}
