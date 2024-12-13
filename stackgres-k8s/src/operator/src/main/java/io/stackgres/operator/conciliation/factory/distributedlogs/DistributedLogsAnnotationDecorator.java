/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractAnnotationDecorator;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class DistributedLogsAnnotationDecorator
    extends AbstractAnnotationDecorator<StackGresDistributedLogsContext> {

  @Override
  protected @NotNull Map<String, String> getAllResourcesAnnotations(
      @NotNull StackGresDistributedLogsContext context) {
    var allResourcesAnnotations = Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getAllResources)
        .orElse(Map.of());

    final Map<String, String> clusterAnnotations =
        context.getSource().getMetadata().getAnnotations();

    return ImmutableMap.<String, String>builder()
        .putAll(allResourcesAnnotations)
        .put(StackGresContext.VERSION_KEY,
            Optional.ofNullable(clusterAnnotations.get(StackGresContext.VERSION_KEY))
            .orElse(StackGresProperty.OPERATOR_VERSION.getString()))
        .build();
  }

  @Override
  protected @NotNull Map<String, String> getServiceAnnotations(
      @NotNull StackGresDistributedLogsContext context) {
    Map<String, String> servicesSpecificAnnotations =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getServices)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(servicesSpecificAnnotations)
        .build();
  }

  protected @NotNull Map<String, String> getPrimaryServiceAnnotations(
      @NotNull StackGresDistributedLogsContext context) {
    Map<String, String> primaryServiceAnnotations =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getPrimaryService)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(context))
        .putAll(primaryServiceAnnotations)
        .build();
  }

  protected @NotNull Map<String, String> getReplicasServiceAnnotations(
      @NotNull StackGresDistributedLogsContext context) {
    Map<String, String> replicaServiceAnnotations =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getReplicasService)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getServiceAnnotations(context))
        .putAll(replicaServiceAnnotations)
        .build();
  }

  @Override
  protected void decorateService(
      @NotNull StackGresDistributedLogsContext context,
      @NotNull HasMetadata service) {
    Map<String, String> customServiceAnnotations;

    customServiceAnnotations = getServiceAnnotations(context);

    decorateResource(service, customServiceAnnotations);
  }

  @Override
  protected @NotNull Map<String, String> getPodAnnotations(
      @NotNull StackGresDistributedLogsContext context) {
    Map<String, String> podSpecificAnnotations =
        Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getClusterPods)
        .orElse(Map.of());

    return ImmutableMap.<String, String>builder()
        .putAll(getAllResourcesAnnotations(context))
        .putAll(podSpecificAnnotations)
        .build();
  }

}
