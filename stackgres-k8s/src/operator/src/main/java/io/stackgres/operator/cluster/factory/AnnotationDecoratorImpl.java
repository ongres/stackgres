/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;

@ApplicationScoped
public class AnnotationDecoratorImpl implements AnnotationDecorator {

  @Override
  public void decorate(StackGresCluster cluster, Iterable<? extends HasMetadata> resources) {

    Map<String, String> allResourcesAnnotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getAllResources)
        .orElse(ImmutableMap.of());

    Map<String, String> servicesSpecificAnnotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getServices)
        .orElse(ImmutableMap.of());

    Map<String, String> podSpecificAnnotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getPods)
        .orElse(ImmutableMap.of());

    Map<String, String> servicesAnnotations = ImmutableMap.<String, String>builder()
        .putAll(allResourcesAnnotations)
        .putAll(servicesSpecificAnnotations)
        .build();

    Map<String, String> podAnnotations = ImmutableMap.<String, String>builder()
        .putAll(allResourcesAnnotations)
        .putAll(podSpecificAnnotations)
        .build();

    resources.forEach(resource -> {
      Map<String, String> resourceAnnotations = Optional.ofNullable(resource.getMetadata())
          .map(ObjectMeta::getAnnotations)
          .orElse(new HashMap<>());

      switch (resource.getKind()) {
        case "Service":
          resourceAnnotations.putAll(servicesAnnotations);
          if (resource.getMetadata().getName().endsWith(PatroniUtil.READ_WRITE_SERVICE)) {
            Map<String, String> primaryServiceAnnotations = Optional.ofNullable(cluster.getSpec())
                .map(StackGresClusterSpec::getPostgresServices)
                .map(StackGresClusterPostgresServices::getPrimary)
                .map(StackGresClusterPostgresService::getAnnotations)
                .orElse(ImmutableMap.of());
            resourceAnnotations.putAll(primaryServiceAnnotations);
          }
          if (resource.getMetadata().getName().endsWith(PatroniUtil.READ_ONLY_SERVICE)) {
            Map<String, String> replicaServiceAnnotations = Optional.ofNullable(cluster.getSpec())
                .map(StackGresClusterSpec::getPostgresServices)
                .map(StackGresClusterPostgresServices::getReplicas)
                .map(StackGresClusterPostgresService::getAnnotations)
                .orElse(ImmutableMap.of());
            resourceAnnotations.putAll(replicaServiceAnnotations);
          }

          break;
        case "Pod":
          resourceAnnotations.putAll(podAnnotations);
          break;
        case "StatefulSet":
          StatefulSet statefulSet = (StatefulSet) resource;
          Map<String, String> podTemplateAnnotations = Optional.ofNullable(statefulSet.getSpec())
              .map(StatefulSetSpec::getTemplate)
              .map(PodTemplateSpec::getMetadata)
              .map(ObjectMeta::getAnnotations)
              .orElse(new HashMap<>());

          podTemplateAnnotations.putAll(podAnnotations);
          Optional.ofNullable(statefulSet.getSpec())
              .map(StatefulSetSpec::getTemplate)
              .ifPresent(template -> {
                if (template.getMetadata() != null) {
                  template.getMetadata()
                      .setAnnotations(podTemplateAnnotations);
                } else {
                  final ObjectMeta metadata = new ObjectMeta();
                  metadata.setAnnotations(podTemplateAnnotations);
                  template.setMetadata(metadata);
                }
              });

          decorate(cluster, statefulSet.getSpec().getVolumeClaimTemplates());
          resourceAnnotations.putAll(allResourcesAnnotations);
          break;
        default:
          resourceAnnotations.putAll(allResourcesAnnotations);
      }
      resource.getMetadata().setAnnotations(resourceAnnotations);

    });

  }
}
