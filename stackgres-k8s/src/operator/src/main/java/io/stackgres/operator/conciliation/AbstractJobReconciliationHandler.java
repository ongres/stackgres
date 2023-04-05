/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import io.stackgres.common.labels.LabelFactory;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJobReconciliationHandler<T extends CustomResource<?, ?>>
    implements ReconciliationHandler<T> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractJobReconciliationHandler.class);

  private final ReconciliationHandler<T> handler;

  private final LabelFactory<T> labelFactory;

  private final ResourceFinder<Job> jobFinder;

  private final ResourceScanner<Pod> podScanner;

  protected AbstractJobReconciliationHandler(
      ReconciliationHandler<T> handler,
      LabelFactory<T> labelFactory,
      ResourceFinder<Job> jobFinder,
      ResourceScanner<Pod> podScanner) {
    this.handler = handler;
    this.labelFactory = labelFactory;
    this.jobFinder = jobFinder;
    this.podScanner = podScanner;
  }

  public AbstractJobReconciliationHandler() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.handler = null;
    this.labelFactory = null;
    this.jobFinder = null;
    this.podScanner = null;
  }

  @Override
  public HasMetadata create(T context, HasMetadata resource) {
    if (isAlreadyCompleted(context)) {
      LOGGER.debug("Skipping creating Job {}.{}",
          resource.getMetadata().getNamespace(),
          resource.getMetadata().getName());
      return resource;
    }
    return concileJob(context, resource, (c, job) -> (Job) handler.create(c, job));
  }

  @Override
  public HasMetadata patch(T context, HasMetadata newResource,
      HasMetadata oldResource) {
    if (isAlreadyCompleted(context)) {
      LOGGER.debug("Skipping patching Job {}.{}",
          oldResource.getMetadata().getNamespace(),
          oldResource.getMetadata().getName());
      return oldResource;
    }
    return concileJob(context, newResource, this::updateJob);
  }

  protected abstract boolean isAlreadyCompleted(T resource);

  @Override
  public HasMetadata replace(T context, HasMetadata resource) {
    LOGGER.warn("Skipping replacing Job {}.{}",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    return resource;
  }

  @Override
  public void delete(T context, HasMetadata resource) {
    if (isAlreadyCompleted(context)) {
      LOGGER.debug("Skipping deleting Job {}.{}",
          resource.getMetadata().getNamespace(),
          resource.getMetadata().getName());
      return;
    }
    handler.delete(context, safeCast(resource));
  }

  @Override
  public void deleteWithOrphans(T context, HasMetadata resource) {
    if (isAlreadyCompleted(context)) {
      LOGGER.debug("Skipping deleting Job {}.{}",
          resource.getMetadata().getNamespace(),
          resource.getMetadata().getName());
      return;
    }
    handler.deleteWithOrphans(context, safeCast(resource));
  }

  private Job safeCast(HasMetadata resource) {
    if (!(resource instanceof Job)) {
      throw new IllegalArgumentException("Resource must be a Job instance");
    }
    return (Job) resource;
  }

  private Job updateJob(T context, Job requiredJob) {
    return KubernetesClientUtil.retryOnConflict(() -> {
      Job currentJob = jobFinder
          .findByNameAndNamespace(
              requiredJob.getMetadata().getName(),
              requiredJob.getMetadata().getNamespace())
          .orElseThrow();
      return (Job) handler.patch(context, fixJobAnnotations(requiredJob, currentJob), null);
    });
  }

  private Job concileJob(T context, HasMetadata resource,
      BiFunction<T, Job, Job> writer) {
    final Job requiredJob = safeCast(resource);
    final Map<String, String> labels = labelFactory.genericLabels(context);

    final String namespace = resource.getMetadata().getNamespace();

    Job updatedJob = writer.apply(context, requiredJob);

    fixPods(context, requiredJob, labels, namespace);

    return updatedJob;
  }

  private Job fixJobAnnotations(Job requiredJob, Job currentJob) {
    var requiredJobAnnotations =
        Optional.ofNullable(requiredJob.getMetadata().getAnnotations())
            .orElse(Map.of());

    return Optional.of(currentJob)
        .filter(job -> requiredJobAnnotations.entrySet().stream()
            .anyMatch(requiredAnnotation -> Optional.ofNullable(job.getMetadata().getAnnotations())
                .stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .noneMatch(podAnnotation -> Objects.equals(requiredAnnotation, podAnnotation))))
        .map(job -> fixJobAnnotations(requiredJobAnnotations, job))
        .orElse(currentJob);
  }

  private Job fixJobAnnotations(Map<String, String> requiredJobAnnotations, Job job) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = job.getMetadata().getNamespace();
      final String jobName = job.getMetadata().getName();
      LOGGER.debug("Fixing annotations for Job {}.{} to {}",
          namespace, jobName, requiredJobAnnotations);
    }
    job.getMetadata().setAnnotations(Optional.ofNullable(job.getMetadata().getAnnotations())
        .map(Seq::seq)
        .orElse(Seq.of())
        .filter(annotation -> requiredJobAnnotations.keySet()
            .stream().noneMatch(annotation.v1::equals))
        .append(Seq.seq(requiredJobAnnotations))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)));
    return job;
  }

  private void fixPods(T context, Job requiredJob,
      final Map<String, String> labels, final String namespace) {
    var podsToFix = podScanner.findByLabelsAndNamespace(namespace, labels).stream()
        .sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .toList();
    List<Pod> podAnnotationsToPatch = fixPodsAnnotations(requiredJob, podsToFix);
    Seq.seq(podAnnotationsToPatch)
        .grouped(pod -> pod.getMetadata().getName()).map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get).forEach(pod -> handler.patch(context, pod, null));
  }

  private List<Pod> fixPodsAnnotations(Job requiredJob, List<Pod> pods) {
    var requiredPodAnnotations =
        Optional.ofNullable(requiredJob.getSpec().getTemplate().getMetadata().getAnnotations())
            .orElse(Map.of());

    return pods.stream()
        .filter(pod -> requiredPodAnnotations.entrySet().stream()
            .anyMatch(requiredAnnotation -> Optional.ofNullable(pod.getMetadata().getAnnotations())
                .stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .noneMatch(podAnnotation -> Objects.equals(requiredAnnotation, podAnnotation))))
        .map(pod -> fixPodAnnotations(requiredPodAnnotations, pod))
        .toList();
  }

  private Pod fixPodAnnotations(Map<String, String> requiredPodAnnotations, Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing annotations for Pod {}.{} for Job {}.{}"
          + " to {}", namespace, podName, namespace, name, requiredPodAnnotations);
    }
    pod.getMetadata().setAnnotations(Optional.ofNullable(pod.getMetadata().getAnnotations())
        .map(Seq::seq)
        .orElse(Seq.of())
        .filter(annotation -> requiredPodAnnotations.keySet()
            .stream().noneMatch(annotation.v1::equals))
        .append(Seq.seq(requiredPodAnnotations))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)));
    return pod;
  }

}
