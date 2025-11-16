/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSpec;
import io.stackgres.common.crd.sgstream.StackGresStreamSpecAnnotations;
import io.stackgres.common.crd.sgstream.StackGresStreamSpecLabels;
import io.stackgres.common.crd.sgstream.StackGresStreamSpecMetadata;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.AbstractMetadataDecorator;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class StreamMetadataDecorator
    extends AbstractMetadataDecorator<StackGresStream> {

  @Override
  protected @NotNull Map<String, String> getAllResourcesAnnotations(
      @NotNull StackGresStream context) {
    var allResourcesAnnotations =
        Optional.of(context)
        .map(StackGresStream::getSpec)
        .map(StackGresStreamSpec::getMetadata)
        .map(StackGresStreamSpecMetadata::getAnnotations)
        .map(StackGresStreamSpecAnnotations::getAllResources)
        .orElse(Map.of());

    final Map<String, String> clusterAnnotations =
        Optional.of(context)
        .map(StackGresStream::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(allResourcesAnnotations)
        .put(StackGresContext.VERSION_KEY,
            Optional.ofNullable(clusterAnnotations.get(StackGresContext.VERSION_KEY))
            .orElse(StackGresProperty.OPERATOR_VERSION.getString()))
        .build();
  }

  @Override
  protected @NotNull Map<String, String> getAllResourcesLabels(
      @NotNull StackGresStream context) {
    return Optional.of(context)
        .map(StackGresStream::getSpec)
        .map(StackGresStreamSpec::getMetadata)
        .map(StackGresStreamSpecMetadata::getLabels)
        .map(StackGresStreamSpecLabels::getAllResources)
        .orElse(Map.of());
  }

}
