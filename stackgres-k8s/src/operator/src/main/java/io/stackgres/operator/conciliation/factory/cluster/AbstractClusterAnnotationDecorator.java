/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobSpec;
import io.fabric8.kubernetes.api.model.batch.v1beta1.JobTemplateSpec;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.operator.conciliation.factory.AnnotationDecorator;
import org.jetbrains.annotations.NotNull;

public class AbstractClusterAnnotationDecorator extends AnnotationDecorator<StackGresCluster> {

  @Override
  protected @NotNull Map<String, String> getAllResourcesAnnotations(
      @NotNull StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getAllResources)
        .orElse(Map.of());
  }

  @Override
  protected @NotNull Map<String, String> getServiceAnnotations(@NotNull StackGresCluster cluster) {

    Map<String, String> servicesSpecificAnnotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getServices)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(cluster))
        .putAll(servicesSpecificAnnotations)
        .build();
  }

  protected @NotNull Map<String, String> getPrimaryServiceAnnotations(
      @NotNull StackGresCluster cluster) {

    Map<String, String> primaryServiceAnnotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getPrimaryService)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(cluster))
        .putAll(primaryServiceAnnotations)
        .build();
  }

  protected @NotNull Map<String, String> getReplicasServiceAnnotations(
      @NotNull StackGresCluster cluster) {

    Map<String, String> replicaServiceAnnotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getReplicasService)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(cluster))
        .putAll(replicaServiceAnnotations)
        .build();
  }

  @Override
  protected void decorateService(@NotNull StackGresCluster cluster, @NotNull HasMetadata service) {

    Map<String, String> customServiceAnnotations;

    final String serviceName = service.getMetadata().getName();
    if (serviceName.endsWith(PatroniUtil.READ_WRITE_SERVICE)) {
      customServiceAnnotations = getPrimaryServiceAnnotations(cluster);
    } else if (serviceName.endsWith(PatroniUtil.READ_ONLY_SERVICE)) {
      customServiceAnnotations = getReplicasServiceAnnotations(cluster);
    } else {
      customServiceAnnotations = getServiceAnnotations(cluster);
    }

    decorateResource(service, customServiceAnnotations);

  }

  @Override
  protected @NotNull Map<String, String> getPodAnnotations(@NotNull StackGresCluster cluster) {

    Map<String, String> podSpecificAnnotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getClusterPods)
        .orElse(Map.of());

    final Map<String, String> clusterAnnotations = cluster.getMetadata().getAnnotations();

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(cluster))
        .putAll(podSpecificAnnotations)
        .put(StackGresContext.VERSION_KEY, clusterAnnotations.get(StackGresContext.VERSION_KEY))
        .build();
  }

  protected @NotNull Map<String, String> getJobAnnotations(@NotNull StackGresCluster cluster) {
    Map<String, String> podSpecificAnnotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getClusterPods)
        .orElse(Map.of());
    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(cluster))
        .putAll(podSpecificAnnotations)
        .put(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString())
        .build();
  }

  protected void decorateJob(@NotNull StackGresCluster cluster, @NotNull HasMetadata resource) {
    Job job = (Job) resource;

    Map<String, String> jobPodTemplateAnnotations = Optional
        .ofNullable(job.getSpec())
        .map(JobSpec::getTemplate)
        .map(PodTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    jobPodTemplateAnnotations.putAll(getJobAnnotations(cluster));

    Optional.ofNullable(job.getSpec())
        .map(JobSpec::getTemplate)
        .ifPresent(podTemplate -> {
          final ObjectMeta podTemplateMetadata = Optional
              .ofNullable(podTemplate.getMetadata())
              .orElse(new ObjectMeta());
          podTemplateMetadata.setAnnotations(jobPodTemplateAnnotations);
          podTemplate.setMetadata(podTemplateMetadata);
        });
    decorateResource(job, getAllResourcesAnnotations(cluster));
  }

  protected void decorateCronJob(@NotNull StackGresCluster cluster, @NotNull HasMetadata resource) {
    CronJob cronJob = (CronJob) resource;

    Map<String, String> jobTemplateAnnotations = Optional.ofNullable(cronJob.getSpec())
        .map(CronJobSpec::getJobTemplate)
        .map(JobTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());
    jobTemplateAnnotations.putAll(getAllResourcesAnnotations(cluster));

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

          cronJobPodTemplateAnnotations.putAll(getPodAnnotations(cluster));

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
    decorateResource(cronJob, getAllResourcesAnnotations(cluster));
  }

  @Override
  protected @NotNull Map<String, BiConsumer<StackGresCluster, HasMetadata>> getCustomDecorators() {
    return ImmutableMap.<String, BiConsumer<StackGresCluster, HasMetadata>>builder()
        .putAll(super.getCustomDecorators())
        .put("Job", this::decorateJob)
        .put("CronJob", this::decorateCronJob)
        .build();
  }

}
