/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractClusterMetadataDecorator<T>
    extends AbstractMetadataDecorator<T> {

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
  protected @NotNull Map<String, String> getAllResourcesLabels(
      @NotNull T context) {
    return getSpecMetadata(context)
        .map(StackGresClusterSpecMetadata::getLabels)
        .map(StackGresClusterSpecLabels::getAllResources)
        .orElse(Map.of());
  }

}
