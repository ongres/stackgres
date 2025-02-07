/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractShardedClusterAnnotationDecorator<T>
    extends AbstractAnnotationDecorator<T> {

  protected abstract StackGresShardedCluster getShardedCluster(T context);

  @Override
  protected @NotNull Map<String, String> getAllResourcesAnnotations(
      @NotNull T context) {
    var allResourcesAnnotations = Optional.ofNullable(getShardedCluster(context).getSpec())
        .map(StackGresShardedClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getAllResources)
        .orElse(Map.of());

    final Map<String, String> clusterAnnotations =
        getShardedCluster(context).getMetadata().getAnnotations();

    return ImmutableMap.<String, String>builder()
        .putAll(allResourcesAnnotations)
        .put(StackGresContext.VERSION_KEY,
            Optional.ofNullable(clusterAnnotations.get(StackGresContext.VERSION_KEY))
            .orElse(StackGresProperty.OPERATOR_VERSION.getString()))
        .build();
  }

  @Override
  protected @NotNull Map<String, String> getServiceAnnotations(
      @NotNull T context) {
    Map<String, String> servicesSpecificAnnotations =
        Optional.ofNullable(getShardedCluster(context).getSpec())
        .map(StackGresShardedClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getServices)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(servicesSpecificAnnotations)
        .build();
  }

  protected @NotNull Map<String, String> getPrimaryServiceAnnotations(
      @NotNull T context) {
    Map<String, String> primaryServiceAnnotations =
        Optional.ofNullable(getShardedCluster(context).getSpec())
        .map(StackGresShardedClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getPrimaryService)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(context))
        .putAll(primaryServiceAnnotations)
        .build();
  }

  protected @NotNull Map<String, String> getReplicasServiceAnnotations(
      @NotNull T context) {
    Map<String, String> replicaServiceAnnotations =
        Optional.ofNullable(getShardedCluster(context).getSpec())
        .map(StackGresShardedClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getReplicasService)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(context))
        .putAll(replicaServiceAnnotations)
        .build();
  }

  @Override
  protected void decorateService(@NotNull T context,
      @NotNull HasMetadata service) {
    Map<String, String> customServiceAnnotations;

    final String serviceName = service.getMetadata().getName();
    if (serviceName.endsWith(PatroniUtil.READ_ONLY_SERVICE)) {
      customServiceAnnotations = getReplicasServiceAnnotations(context);
    } else {
      customServiceAnnotations = getServiceAnnotations(context);
    }

    decorateResource(service, customServiceAnnotations);
  }

  @Override
  protected @NotNull Map<String, String> getPodAnnotations(
      @NotNull T context) {
    Map<String, String> podSpecificAnnotations =
        Optional.ofNullable(getShardedCluster(context).getSpec())
        .map(StackGresShardedClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getClusterPods)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(podSpecificAnnotations)
        .build();
  }

}
