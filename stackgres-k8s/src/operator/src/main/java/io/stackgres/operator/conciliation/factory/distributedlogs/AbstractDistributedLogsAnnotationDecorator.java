/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecAnnotations;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecMetadata;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AnnotationDecorator;
import org.jetbrains.annotations.NotNull;

public class AbstractDistributedLogsAnnotationDecorator
    extends AnnotationDecorator<StackGresDistributedLogsContext> {

  @Override
  protected @NotNull Map<String, String> getAllResourcesAnnotations(
      @NotNull StackGresDistributedLogsContext context) {
    return Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresDistributedLogsSpecMetadata::getAnnotations)
        .map(StackGresDistributedLogsSpecAnnotations::getAllResources)
        .orElse(Map.of());
  }

  @Override
  protected @NotNull Map<String, String> getServiceAnnotations(
      @NotNull StackGresDistributedLogsContext context) {
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
  protected @NotNull Map<String, String> getPodAnnotations(
      @NotNull StackGresDistributedLogsContext context) {
    return Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresDistributedLogsSpecMetadata::getAnnotations)
        .map(StackGresDistributedLogsSpecAnnotations::getPods)
        .orElse(Map.of());
  }
}
