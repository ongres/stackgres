/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static io.stackgres.operator.conciliation.ReconciliationUtil.isResourceReconciliationNotPaused;

import java.util.List;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConciliator<T extends CustomResource<?, ?>> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractConciliator.class);

  private final RequiredResourceGenerator<T> requiredResourceGenerator;
  private final AbstractDeployedResourcesScanner<T> deployedResourceScanner;
  protected final DeployedResourcesCache deployedResourcesCache;

  protected AbstractConciliator(
      RequiredResourceGenerator<T> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<T> deployedResourceScanner,
      DeployedResourcesCache deployedResourcesCache) {
    this.requiredResourceGenerator = requiredResourceGenerator;
    this.deployedResourceScanner = deployedResourceScanner;
    this.deployedResourcesCache = deployedResourcesCache;
  }

  public AbstractConciliator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.requiredResourceGenerator = null;
    this.deployedResourceScanner = null;
    this.deployedResourcesCache = null;
  }

  public ReconciliationResult evalReconciliationState(T config) {
    List<HasMetadata> requiredResources = requiredResourceGenerator.getRequiredResources(config);

    DeployedResourcesSnapshot deployedResourcesSnapshot =
        deployedResourceScanner.getDeployedResources(config);

    List<HasMetadata> creations = requiredResources.stream()
        .filter(Predicate.not(deployedResourcesSnapshot::isDeployed))
        .toList();

    SkipDeletion skipDeletion = new SkipDeletion(config, requiredResources);
    List<HasMetadata> deletions = deployedResourcesSnapshot.ownedDeployedResources().stream()
        .map(deployedResourcesSnapshot::get)
        .map(DeployedResource::foundDeployed)
        .filter(Predicate.not(skipDeletion))
        .toList();

    ForcedChange forcedChange = new ForcedChange(
        config, deployedResourcesSnapshot);
    SkipUpdate skipUpdate = new SkipUpdate();
    List<Tuple2<HasMetadata, HasMetadata>> patches = requiredResources.stream()
        .map(Tuple::tuple)
        .map(t -> t.concat(t.v1))
        .map(t -> t.map2(deployedResourcesSnapshot::get))
        .filter(t -> t.v2 != null)
        .filter(forcedChange)
        .filter(Predicate.not(skipUpdate))
        .map(t -> t.map2(DeployedResource::foundDeployed))
        .toList();

    return new ReconciliationResult(
        creations,
        patches,
        deletions);
  }

  protected boolean skipDeletion(HasMetadata requiredResource, T config) {
    return false;
  }

  class SkipDeletion implements Predicate<HasMetadata> {
    final T config;
    final List<HasMetadata> requiredResources;

    public SkipDeletion(T config, List<HasMetadata> requiredResources) {
      this.config = config;
      this.requiredResources = requiredResources;
    }

    @Override
    public boolean test(HasMetadata foundDeployedResource) {
      boolean result = skipDeletion(foundDeployedResource, config)
          || !isResourceReconciliationNotPaused(foundDeployedResource);
      if (result && LOGGER.isTraceEnabled()) {
        LOGGER.trace("Skip deletion for resource {} {}.{}",
            foundDeployedResource.getKind(),
            foundDeployedResource.getMetadata().getNamespace(),
            foundDeployedResource.getMetadata().getName());
      }
      return result || !requireDeletion(foundDeployedResource);
    }

    private boolean requireDeletion(HasMetadata foundDeployedResource) {
      boolean result = requiredResources.stream()
          .noneMatch(required -> ResourceKey.same(required, foundDeployedResource));
      if (result && LOGGER.isTraceEnabled()) {
        LOGGER.trace("Detected deletion for resource {} {}.{}",
            foundDeployedResource.getKind(),
            foundDeployedResource.getMetadata().getNamespace(),
            foundDeployedResource.getMetadata().getName());
      }
      return result;
    }
  }

  protected boolean forceChange(HasMetadata requiredResource, T config) {
    return false;
  }

  class ForcedChange implements Predicate<Tuple2<HasMetadata, DeployedResource>> {
    final T config;
    final DeployedResourcesSnapshot deployedResourcesSnapshot;

    public ForcedChange(T config, DeployedResourcesSnapshot deployedResourcesSnapshot) {
      this.config = config;
      this.deployedResourcesSnapshot = deployedResourcesSnapshot;
    }

    @Override
    public boolean test(
        Tuple2<HasMetadata, DeployedResource> requiredAndDeployedResourceValue) {
      HasMetadata requiredResource = requiredAndDeployedResourceValue.v1;
      boolean result = forceChange(requiredResource, config);
      if (result && LOGGER.isTraceEnabled()) {
        LOGGER.trace("Forced change for resource {} {}.{}",
            requiredResource.getKind(),
            requiredResource.getMetadata().getNamespace(),
            requiredResource.getMetadata().getName());
      }
      return result || deployedResourcesSnapshot.isRequiredChanged(requiredResource)
          || deployedResourcesSnapshot.isDeployedChanged(requiredAndDeployedResourceValue.v2);
    }
  }

  protected boolean skipUpdate(HasMetadata requiredResource, T config) {
    return false;
  }

  class SkipUpdate implements Predicate<Tuple2<HasMetadata, DeployedResource>> {
    @Override
    public boolean test(
        Tuple2<HasMetadata, DeployedResource> requiredAndDeployedResourceValue) {
      HasMetadata foundDeployedResource = requiredAndDeployedResourceValue.v2.foundDeployed();
      boolean result = !isResourceReconciliationNotPaused(foundDeployedResource);
      if (result && LOGGER.isTraceEnabled()) {
        LOGGER.trace("Skip update for resource {} {}.{}",
            foundDeployedResource.getKind(),
            foundDeployedResource.getMetadata().getNamespace(),
            foundDeployedResource.getMetadata().getName());
      }
      return result;
    }
  }
}
