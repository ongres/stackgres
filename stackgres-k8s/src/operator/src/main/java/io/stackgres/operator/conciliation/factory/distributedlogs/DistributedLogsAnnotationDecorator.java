/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecAnnotations;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecMetadata;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractAnnotationDecorator;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class DistributedLogsAnnotationDecorator
    extends AbstractAnnotationDecorator<StackGresDistributedLogsContext> {

  @Override
  protected @Nonnull Map<String, String> getAllResourcesAnnotations(
      @Nonnull StackGresDistributedLogsContext context) {
    var allResourcesAnnotations = Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresDistributedLogsSpecMetadata::getAnnotations)
        .map(StackGresDistributedLogsSpecAnnotations::getAllResources)
        .orElse(Map.of());

    final Map<String, String> distributedLogsAnnotations =
        context.getSource().getMetadata().getAnnotations();

    return ImmutableMap.<String, String>builder()
        .putAll(allResourcesAnnotations)
        .put(StackGresContext.VERSION_KEY,
            Optional.ofNullable(distributedLogsAnnotations.get(StackGresContext.VERSION_KEY))
            .orElse(StackGresProperty.OPERATOR_VERSION.getString()))
        .build();
  }

  @Override
  protected @Nonnull Map<String, String> getServiceAnnotations(
      @Nonnull StackGresDistributedLogsContext context) {
    Map<String, String> servicesSpecificAnnotations =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresDistributedLogsSpecMetadata::getAnnotations)
        .map(StackGresDistributedLogsSpecAnnotations::getServices)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(servicesSpecificAnnotations)
        .build();
  }

  @Override
  protected @Nonnull Map<String, String> getPodAnnotations(
      @Nonnull StackGresDistributedLogsContext context) {
    return Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresDistributedLogsSpecMetadata::getAnnotations)
        .map(StackGresDistributedLogsSpecAnnotations::getPods)
        .orElse(Map.of());
  }
}
