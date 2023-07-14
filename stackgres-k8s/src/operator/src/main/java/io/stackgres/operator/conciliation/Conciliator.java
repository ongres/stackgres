/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public class Conciliator<T extends CustomResource<?, ?>> {

  private RequiredResourceGenerator<T> requiredResourceGenerator;

  private DeployedResourcesScanner<T> deployedResourcesScanner;

  private ComparisonDelegator<T> resourceComparator;

  public ReconciliationResult evalReconciliationState(T config) {
    var requiredResources = requiredResourceGenerator.getRequiredResources(config);
    var deployedResources = deployedResourcesScanner.getDeployedResources(config);

    var creations = requiredResources.stream()
        .filter(requiredResource -> deployedResources.stream()
            .noneMatch(deployedResource -> isTheSameResource(requiredResource, deployedResource)))
        .collect(Collectors.toUnmodifiableList());

    var deletions = deployedResources.stream()
        .filter(deployedResource -> requiredResources.stream()
            .noneMatch(requiredResource -> isTheSameResource(deployedResource, requiredResource)))
        .filter(ReconciliationUtil::isResourceReconciliationNotPaused)
        .collect(Collectors.toUnmodifiableList());

    List<Tuple2<HasMetadata, HasMetadata>> patches = requiredResources.stream()
        .map(requiredResource -> deployedResources.stream()
              .filter(dr -> isTheSameResource(requiredResource, dr))
              .findFirst()
              .map(deployedResource -> Tuple.tuple(
                  requiredResource, Optional.of(deployedResource)))
              .orElseGet(() -> Tuple.tuple(
                  requiredResource, Optional.empty())))
        .filter(resourceTuple -> resourceTuple.v2.isPresent())
        .map(rt -> rt.map2(Optional::get))
        .filter(tuple -> ReconciliationUtil.isResourceReconciliationNotPaused(tuple.v2))
        .filter(resourceTuple -> !isResourceContentEqual(resourceTuple.v1, resourceTuple.v2))
        .collect(Collectors.toUnmodifiableList());

    return new ReconciliationResult(creations,
        patches,
        deletions);
  }

  protected boolean isResourceContentEqual(HasMetadata r1, HasMetadata r2) {
    return resourceComparator.isResourceContentEqual(r1, r2);
  }

  protected boolean isTheSameResource(HasMetadata r1, HasMetadata r2) {
    return resourceComparator.isTheSameResource(r1, r2);
  }

  @Inject
  public void setRequiredResourceGenerator(RequiredResourceGenerator<T> requiredResourceGenerator) {
    this.requiredResourceGenerator = requiredResourceGenerator;
  }

  @Inject
  public void setDeployedResourcesScanner(DeployedResourcesScanner<T> deployedResourcesScanner) {
    this.deployedResourcesScanner = deployedResourcesScanner;
  }

  @Inject
  public void setResourceComparator(ComparisonDelegator<T> resourceComparator) {
    this.resourceComparator = resourceComparator;
  }
}
