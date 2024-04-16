/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;

public abstract class AbstractClusterAnnotationDecorator<T>
    extends AbstractAnnotationDecorator<T> {

  protected abstract StackGresCluster getCluster(T context);

  @Override
  protected @Nonnull Map<String, String> getAllResourcesAnnotations(
      @Nonnull T context) {
    var allResourcesAnnotations = Optional.ofNullable(getCluster(context).getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getAllResources)
        .orElse(Map.of());

    final Map<String, String> clusterAnnotations =
        getCluster(context).getMetadata().getAnnotations();

    return ImmutableMap.<String, String>builder()
        .putAll(allResourcesAnnotations)
        .put(StackGresContext.VERSION_KEY,
            Optional.ofNullable(clusterAnnotations.get(StackGresContext.VERSION_KEY))
            .orElse(StackGresProperty.OPERATOR_VERSION.getString()))
        .build();
  }

  @Override
  protected @Nonnull Map<String, String> getServiceAnnotations(
      @Nonnull T context) {
    Map<String, String> servicesSpecificAnnotations =
        Optional.ofNullable(getCluster(context).getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getServices)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(servicesSpecificAnnotations)
        .build();
  }

  protected @Nonnull Map<String, String> getPrimaryServiceAnnotations(
      @Nonnull T context) {
    Map<String, String> primaryServiceAnnotations =
        Optional.ofNullable(getCluster(context).getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getPrimaryService)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(context))
        .putAll(primaryServiceAnnotations)
        .build();
  }

  protected @Nonnull Map<String, String> getReplicasServiceAnnotations(
      @Nonnull T context) {
    Map<String, String> replicaServiceAnnotations =
        Optional.ofNullable(getCluster(context).getSpec())
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
  protected void decorateService(@Nonnull T context,
      @Nonnull HasMetadata service) {
    Map<String, String> customServiceAnnotations;

    final String serviceName = service.getMetadata().getName();
    if (serviceName.endsWith(PatroniUtil.DEPRECATED_READ_WRITE_SERVICE)) {
      customServiceAnnotations = getPrimaryServiceAnnotations(context);
    } else if (serviceName.endsWith(PatroniUtil.READ_ONLY_SERVICE)) {
      customServiceAnnotations = getReplicasServiceAnnotations(context);
    } else {
      customServiceAnnotations = getServiceAnnotations(context);
    }

    decorateResource(service, customServiceAnnotations);
  }

  @Override
  protected @Nonnull Map<String, String> getPodAnnotations(
      @Nonnull T context) {
    Map<String, String> podSpecificAnnotations =
        Optional.ofNullable(getCluster(context).getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getClusterPods)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(podSpecificAnnotations)
        .build();
  }

}
