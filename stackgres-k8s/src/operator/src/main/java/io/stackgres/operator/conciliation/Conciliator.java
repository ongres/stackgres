/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import org.jooq.lambda.tuple.Tuple;

public class Conciliator<T extends CustomResource<?, ?>> {

  private RequiredResourceGenerator<T> requiredResourceGenerator;

  private DeployedResourcesScanner<T> deployedResourcesScanner;

  private ComparisonDelegator<T> resourceComparator;

  public ReconciliationResult evalReconciliationState(T config) {
    var requiredResources = requiredResourceGenerator.getRequiredResources(config);
    var deployedResources = deployedResourcesScanner.getDeployedResources(config);

    var creations = requiredResources.stream()
        .filter(requiredResource -> deployedResources.stream()
            .noneMatch(deployedResource -> isTheSameResource(requiredResource, deployedResource)));

    var deletions = deployedResources.stream()
        .filter(deployedResource -> requiredResources.stream()
            .noneMatch(requiredResource -> isTheSameResource(deployedResource, requiredResource)));

    Stream<HasMetadata> patches = requiredResources.stream()
        .map(requiredResource -> {
          Optional<HasMetadata> deployedResource = deployedResources.stream()
              .filter(dr -> isTheSameResource(requiredResource, dr))
              .findFirst();
          return Tuple.tuple(requiredResource, deployedResource);
        }).filter(resourceTuple -> resourceTuple.v2.isPresent())
        .map(rt -> rt.map2(Optional::get))
        .filter(resourceTuple -> !isResourceContentEqual(resourceTuple.v1, resourceTuple.v2))
        .map(resourceTuple -> resourceTuple.v1);

    return new ReconciliationResult(creations.collect(Collectors.toUnmodifiableList()),
        patches.collect(Collectors.toUnmodifiableList()),
        deletions.collect(Collectors.toUnmodifiableList()));

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
