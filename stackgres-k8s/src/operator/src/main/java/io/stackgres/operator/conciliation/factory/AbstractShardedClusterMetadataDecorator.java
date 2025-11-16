/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadata;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractShardedClusterMetadataDecorator<T>
    extends AbstractMetadataDecorator<T> {

  protected abstract StackGresShardedCluster getShardedCluster(T context);

  @Override
  protected @NotNull Map<String, String> getAllResourcesAnnotations(
      @NotNull T context) {
    var allResourcesAnnotations = Optional.ofNullable(getShardedCluster(context).getSpec())
        .map(StackGresShardedClusterSpec::getMetadata)
        .map(StackGresShardedClusterSpecMetadata::getAnnotations)
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
  protected @NotNull Map<String, String> getAllResourcesLabels(
      @NotNull T context) {
    return Optional.ofNullable(getShardedCluster(context).getSpec())
        .map(StackGresShardedClusterSpec::getMetadata)
        .map(StackGresShardedClusterSpecMetadata::getLabels)
        .map(StackGresClusterSpecLabels::getAllResources)
        .orElse(Map.of());
  }

}
