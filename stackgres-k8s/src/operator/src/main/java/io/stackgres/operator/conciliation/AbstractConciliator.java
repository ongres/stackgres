/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static io.stackgres.operator.conciliation.ReconciliationUtil.isResourceReconciliationNotPaused;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.ws.rs.core.Response;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConciliator<T extends CustomResource<?, ?>> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractConciliator.class);

  private final KubernetesClient client;
  private final CustomResourceFinder<T> finder;
  private final RequiredResourceGenerator<T> requiredResourceGenerator;
  private final AbstractDeployedResourcesScanner<T> deployedResourceScanner;
  protected final DeployedResourcesCache deployedResourcesCache;

  protected AbstractConciliator(
      KubernetesClient client,
      CustomResourceFinder<T> finder,
      RequiredResourceGenerator<T> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<T> deployedResourceScanner,
      DeployedResourcesCache deployedResourcesCache) {
    this.client = client;
    this.finder = finder;
    this.requiredResourceGenerator = requiredResourceGenerator;
    this.deployedResourceScanner = deployedResourceScanner;
    this.deployedResourcesCache = deployedResourcesCache;
  }

  public AbstractConciliator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
    this.finder = null;
    this.requiredResourceGenerator = null;
    this.deployedResourceScanner = null;
    this.deployedResourcesCache = null;
  }

  public ReconciliationResult evalReconciliationState(T config) {
    OwnerReferenceMapper ownerReferenceMapper = new OwnerReferenceMapper(config);
    List<HasMetadata> requiredResources = requiredResourceGenerator.getRequiredResources(config)
        .stream()
        .map(ownerReferenceMapper)
        .map(resource -> {
          resource.getMetadata().setManagedFields(null);
          return resource;
        })
        .toList();

    DeployedResourcesSnapshot deployedResourcesSnapshot =
        deployedResourceScanner.getDeployedResources(config, requiredResources);

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

    var deployedOtherOwnerRequiredResources = deployedResourcesSnapshot.deployedResources().stream()
        .filter(deployedResource -> deployedResource.getMetadata().getOwnerReferences() != null
            && !deployedResource.getMetadata().getOwnerReferences().isEmpty())
        .map(ResourceKey::create)
        .filter(deployedResourceKey -> deployedResourcesSnapshot.ownedDeployedResources().stream()
            .map(ResourceKey::create)
            .noneMatch(deployedResourceKey::equals))
        .filter(deployedResourceKey -> requiredResources.stream()
            .map(ResourceKey::create)
            .anyMatch(deployedResourceKey::equals))
        .toList();
    if (!deployedOtherOwnerRequiredResources.isEmpty()) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Following resources are required but already exists in the cluster and are owned by another resource: {}",
            deployedOtherOwnerRequiredResources.stream()
                .map(resourceKey -> resourceKey.kind()
                    + " " + resourceKey.namespace()
                    + "." + resourceKey.name())
                .collect(Collectors.joining(", ")));
      }
      // Workaround for https://github.com/kubernetes/kubernetes/issues/120960
      cleanupNonGarbageCollectedResources(
          deployedResourcesSnapshot, deployedOtherOwnerRequiredResources);
      return new ReconciliationResult(
          List.of(),
          List.of(),
          List.of());
    }

    var foundConfig = finder.findByNameAndNamespace(
        config.getMetadata().getName(),
        config.getMetadata().getNamespace());

    if (foundConfig.isEmpty()) {
      LOGGER.debug("Config {}.{} was deleted aborting reconciliation",
          config.getMetadata().getName(),
          config.getMetadata().getNamespace());
      return new ReconciliationResult(
          List.of(),
          List.of(),
          List.of());
    }

    return new ReconciliationResult(
        creations,
        patches,
        deletions);
  }

  private void cleanupNonGarbageCollectedResources(
      DeployedResourcesSnapshot deployedResourcesSnapshot,
      List<ResourceKey> deployedNonOwnedRequiredResources) {
    deployedNonOwnedRequiredResources.stream()
        .filter(resourceKey -> resourceKey.kind().equals(
            HasMetadata.getKind(ServiceAccount.class)))
        .map(resourceKey -> deployedResourcesSnapshot.deployedResources().stream()
            .filter(deployedResource -> ResourceKey.create(deployedResource).equals(resourceKey))
            .findFirst()
            .orElseThrow())
        .filter(resource -> resource.getMetadata().getOwnerReferences() != null
            && !resource.getMetadata().getOwnerReferences().isEmpty())
        .forEach(resource -> {
          if (resource.getMetadata().getOwnerReferences()
              .stream()
              .noneMatch(ownerReference -> {
                try {
                  var ownerResource = client
                      .genericKubernetesResources(
                          ownerReference.getApiVersion(), ownerReference.getKind())
                      .inNamespace(resource.getMetadata().getNamespace())
                      .withName(ownerReference.getName())
                      .get();
                  return ownerResource != null
                      && ownerResource.getMetadata() != null
                      && Objects.equals(ownerResource.getMetadata().getUid(), ownerReference.getUid());
                } catch (KubernetesClientException ex) {
                  if (ex.getCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                    return false;
                  }
                  throw ex;
                }
              })) {
            LOGGER.warn("Proceding to delete following required resource that already exists in the cluster but was not garbage collected due to a bug (see https://github.com/kubernetes/kubernetes/issues/120960): {}", resource.getKind()
                + " " + resource.getMetadata().getNamespace()
                + "." + resource.getMetadata().getName());
            try {
              client.resource(resource).delete();
            } catch (KubernetesClientException ex) {
              LOGGER.warn("Error while trying to remove ungarbaged resource {} {}.{}",
                  resource.getKind(), resource.getMetadata().getNamespace(),
                  resource.getMetadata().getName(), ex);
            }
          }
        });
  }

  class OwnerReferenceMapper implements Function<HasMetadata, HasMetadata> {
    final T config;
    final List<OwnerReference> ownerReferences;

    public OwnerReferenceMapper(T config) {
      this.config = config;
      this.ownerReferences = List.of(ResourceUtil.getControllerOwnerReference(config));
    }

    @Override
    @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
        justification = "False positive")
    public HasMetadata apply(HasMetadata resource) {
      if (Objects.equals(
          resource.getMetadata().getNamespace(),
          config.getMetadata().getNamespace())
          && resource.getMetadata().getOwnerReferences().isEmpty()) {
        resource.getMetadata().setOwnerReferences(ownerReferences);
      }
      return resource;
    }
  }

  protected boolean skipDeletion(HasMetadata foundDeployedResource, T config) {
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
