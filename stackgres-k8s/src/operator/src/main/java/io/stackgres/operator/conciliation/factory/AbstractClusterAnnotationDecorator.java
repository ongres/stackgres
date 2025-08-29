/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractClusterAnnotationDecorator<T>
    extends AbstractAnnotationDecorator<T> {

  protected abstract Optional<StackGresClusterSpecMetadata> getSpecMetadata(T context);

  protected abstract Optional<ObjectMeta> getMetadata(T context);

  @Override
  protected @NotNull Map<String, String> getAllResourcesAnnotations(
      @NotNull T context) {
    var allResourcesAnnotations =
        getSpecMetadata(context)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getAllResources)
        .orElse(Map.of());

    final Map<String, String> clusterAnnotations =
        getMetadata(context).map(ObjectMeta::getAnnotations).orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(allResourcesAnnotations)
        .put(StackGresContext.VERSION_KEY,
            Optional.ofNullable(clusterAnnotations.get(StackGresContext.VERSION_KEY))
            .orElse(StackGresProperty.OPERATOR_VERSION.getString()))
        .build();
  }

  @Override
  protected @NotNull Map<String, String> getServiceAccountAnnotations(
      @NotNull T context) {
    Map<String, String> serviceAccountsSpecificAnnotations =
        getSpecMetadata(context)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getServiceAccount)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(serviceAccountsSpecificAnnotations)
        .build();
  }

  @Override
  protected @NotNull Map<String, String> getServiceAnnotations(
      @NotNull T context) {
    Map<String, String> servicesSpecificAnnotations =
        getSpecMetadata(context)
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
        getSpecMetadata(context)
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
        getSpecMetadata(context)
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
  protected @NotNull Map<String, String> getPodAnnotations(
      @NotNull T context) {
    Map<String, String> podSpecificAnnotations =
        getSpecMetadata(context)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getClusterPods)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(podSpecificAnnotations)
        .build();
  }

}
