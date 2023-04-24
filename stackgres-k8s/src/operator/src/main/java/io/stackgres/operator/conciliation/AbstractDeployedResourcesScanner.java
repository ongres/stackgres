/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.jooq.lambda.Seq;

public abstract class AbstractDeployedResourcesScanner<T extends CustomResource<?, ?>> {

  private final DeployedResourcesCache deployedResourcesCache;

  protected AbstractDeployedResourcesScanner(DeployedResourcesCache deployedResourcesCache) {
    this.deployedResourcesCache = deployedResourcesCache;
  }

  public DeployedResourcesSnapshot getDeployedResources(T config) {
    final String kind = HasMetadata.getKind(config.getClass());
    final Map<String, String> genericLabels = getGenericLabels(config);
    final Map<String, String> crossNamespaceLabels = getCrossNamespaceLabels(config);

    KubernetesClient client = getClient();

    List<HasMetadata> inNamespace = getInNamepspaceResourceOperations()
        .values()
        .stream()
        .filter(op -> !genericLabels.isEmpty())
        .<HasMetadata>flatMap(emptyOrNotFound(op -> op.apply(client)
            .inNamespace(config.getMetadata().getNamespace())
            .withLabels(genericLabels)
            .list()
            .getItems()
            .stream()))
        .toList();

    List<HasMetadata> inAnyNamespace = getInAnyNamespaceResourceOperations(config)
        .values()
        .stream()
        .filter(op -> !crossNamespaceLabels.isEmpty())
        .<HasMetadata>flatMap(emptyOrNotFound(op -> op.apply(client)
            .inAnyNamespace()
            .withLabels(crossNamespaceLabels)
            .list()
            .getItems()
            .stream()))
        .toList();

    List<HasMetadata> deployedResources = Seq.seq(inNamespace)
        .append(inAnyNamespace)
        .toList();
    List<HasMetadata> ownedDeployedResources = deployedResources.stream()
        .filter(resource -> resource.getMetadata().getOwnerReferences()
            .stream().anyMatch(ownerReference -> ownerReference.getKind().equals(kind)
                && ownerReference.getName().equals(config.getMetadata().getName())
                && ownerReference.getUid().equals(config.getMetadata().getUid())
                && ownerReference.getController() != null && ownerReference.getController()))
        .toList();

    deployedResourcesCache.removeWithLabelsNotIn(genericLabels, deployedResources);
    DeployedResourcesSnapshot deployedResourcesSnapshot =
        deployedResourcesCache.createDeployedResourcesSnapshot(
            ownedDeployedResources, deployedResources);

    return deployedResourcesSnapshot;
  }

  private <P, R> Function<P, Stream<R>> emptyOrNotFound(Function<P, Stream<R>> function) {
    return param -> {
      try {
        return function.apply(param);
      } catch (KubernetesClientException ex) {
        if (ex.getCode() == 404) {
          return Stream.of();
        }
        throw ex;
      }
    };
  }

  protected Map<String, String> getGenericLabels(T config) {
    return Map.of();
  }

  protected Map<String, String> getCrossNamespaceLabels(T config) {
    return Map.of();
  }

  protected abstract KubernetesClient getClient();

  protected Map<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      getInAnyNamespaceResourceOperations(T config) {
    return Map.of();
  }

  protected Map<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getInNamepspaceResourceOperations() {
    return Map.of();
  }

}
