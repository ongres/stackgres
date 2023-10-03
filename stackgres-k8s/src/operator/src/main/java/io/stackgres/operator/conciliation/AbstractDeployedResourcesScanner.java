/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

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

  public DeployedResourcesSnapshot getDeployedResources(
      T config, List<HasMetadata> requiredResources) {
    final String kind = HasMetadata.getKind(config.getClass());
    final Map<String, String> genericLabels = getGenericLabels(config);
    final Map<String, String> crossNamespaceLabels = getCrossNamespaceLabels(config);

    final KubernetesClient client = getClient();

    final var inNamepspaceResourceOperations = getInNamepspaceResourceOperations();
    final List<HasMetadata> inNamespace = inNamepspaceResourceOperations
        .values()
        .stream()
        .filter(op -> !genericLabels.isEmpty())
        .<HasMetadata>flatMap(streamList(op -> op.apply(client)
            .inNamespace(config.getMetadata().getNamespace())
            .withLabels(genericLabels)
            .list()
            .getItems()))
        .toList();
    final List<HasMetadata> inNamespaceRequired = requiredResources
        .stream()
        .filter(requiredResource -> Objects.equals(
            config.getMetadata().getNamespace(),
            requiredResource.getMetadata().getNamespace()))
        .filter(requiredResource -> inNamespace.stream()
            .noneMatch(resource -> Objects.equals(
                resource.getMetadata().getName(),
                requiredResource.getMetadata().getName())))
        .filter(requiredResource -> inNamepspaceResourceOperations
            .containsKey(requiredResource.getClass()))
        .<HasMetadata>flatMap(streamResource(requiredResource -> inNamepspaceResourceOperations
            .get(requiredResource.getClass()).apply(client)
            .inNamespace(requiredResource.getMetadata().getNamespace())
            .withName(requiredResource.getMetadata().getName())
            .get()))
        .toList();

    final var inAnyNamespaceResourceOperations = getInAnyNamespaceResourceOperations(config);
    final List<HasMetadata> inAnyNamespace = inAnyNamespaceResourceOperations
        .values()
        .stream()
        .filter(op -> !crossNamespaceLabels.isEmpty())
        .<HasMetadata>flatMap(streamList(op -> op.apply(client)
            .inAnyNamespace()
            .withLabels(crossNamespaceLabels)
            .list()
            .getItems()))
        .toList();
    final List<HasMetadata> inAnyNamespaceRequired = requiredResources
        .stream()
        .filter(requiredResource -> inNamespace.stream()
            .noneMatch(resource -> Objects.equals(
                resource.getMetadata().getName(),
                requiredResource.getMetadata().getName())
                && Objects.equals(
                    resource.getMetadata().getNamespace(),
                    requiredResource.getMetadata().getNamespace())))
        .filter(requiredResource -> inAnyNamespaceResourceOperations
            .containsKey(requiredResource.getClass()))
        .<HasMetadata>flatMap(streamResource(requiredResource -> inAnyNamespaceResourceOperations
              .get(requiredResource.getClass()).apply(client)
              .inNamespace(requiredResource.getMetadata().getNamespace())
              .withName(requiredResource.getMetadata().getName())
              .get()))
        .filter(Objects::nonNull)
        .toList();

    final List<HasMetadata> deployedResources = Seq.seq(inNamespace)
        .append(inNamespaceRequired)
        .append(inAnyNamespace)
        .append(inAnyNamespaceRequired)
        .toList();
    final List<HasMetadata> ownedDeployedResources = deployedResources.stream()
        .filter(resource -> checkOwnerReference(config, kind, resource))
        .toList();

    deployedResourcesCache.removeWithLabelsNotIn(genericLabels, deployedResources);
    final DeployedResourcesSnapshot deployedResourcesSnapshot =
        deployedResourcesCache.createDeployedResourcesSnapshot(
            ownedDeployedResources, deployedResources);

    return deployedResourcesSnapshot;
  }

  private boolean checkOwnerReference(T config, final String kind, HasMetadata resource) {
    return resource.getMetadata().getOwnerReferences() != null
        && resource.getMetadata().getOwnerReferences()
        .stream()
        .anyMatch(ownerReference -> ownerReference.getKind().equals(kind)
            && ownerReference.getName().equals(config.getMetadata().getName())
            && ownerReference.getUid().equals(config.getMetadata().getUid())
            && ownerReference.getController() != null
            && ownerReference.getController());
  }

  private <P, R> Function<P, Stream<R>> streamResource(Function<P, R> function) {
    return param -> {
      try {
        return Stream.of(function.apply(param))
            .filter(Objects::nonNull);
      } catch (KubernetesClientException ex) {
        if (ex.getCode() == Response.Status.NOT_FOUND.getStatusCode()) {
          return Stream.of();
        }
        throw ex;
      }
    };
  }

  private <P, R> Function<P, Stream<R>> streamList(Function<P, List<R>> function) {
    return param -> {
      try {
        return function.apply(param).stream();
      } catch (KubernetesClientException ex) {
        if (ex.getCode() == Response.Status.NOT_FOUND.getStatusCode()) {
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
