/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceWriter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DeployedResourcesScanner<T extends CustomResource<?, ?>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      DeployedResourcesScanner.class.getPackage().getName());

  public List<HasMetadata> getDeployedResources(T config) {
    final Map<String, String> genericClusterLabels = getGenericLabels(config);

    StackGresKubernetesClient stackGresClient = getClient();

    Stream<HasMetadata> inNamespace = getInNamepspaceResourceOperations()
        .keySet()
        .stream()
        .flatMap(clazz -> stackGresClient.findManagedIntents(
            clazz,
            ResourceWriter.STACKGRES_FIELD_MANAGER,
            genericClusterLabels,
            config.getMetadata().getNamespace())
            .stream());

    Stream<HasMetadata> anyNamespace = getAnyNamespaceResourceOperations()
        .keySet()
        .stream()
        .flatMap(clazz -> stackGresClient.findManagedIntents(
            clazz,
            ResourceWriter.STACKGRES_FIELD_MANAGER,
            genericClusterLabels,
            config.getMetadata().getNamespace())
            .stream());

    List<HasMetadata> deployedResources = Stream.concat(inNamespace, anyNamespace)
        .filter(resource -> resource.getMetadata().getOwnerReferences()
            .stream().anyMatch(ownerReference -> ownerReference.getKind()
                .equals(StackGresCluster.KIND)
                && ownerReference.getName().equals(config.getMetadata().getName())
                && ownerReference.getUid().equals(config.getMetadata().getUid())))
        .collect(Collectors.toUnmodifiableList());

    List<HasMetadata> ownedDeployedResources = deployedResources.stream()
        .map(this::ownResource)
        .collect(Collectors.toUnmodifiableList());

    return ownedDeployedResources;
  }

  protected abstract Map<String, String> getGenericLabels(T config);

  protected abstract StackGresKubernetesClient getClient();

  protected abstract ImmutableMap<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getAnyNamespaceResourceOperations();

  protected abstract ImmutableMap<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getInNamepspaceResourceOperations();

  private HasMetadata ownResource(HasMetadata deployedResource) {
    if (Optional.ofNullable(deployedResource)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .stream()
        .map(Map::keySet)
        .flatMap(Set::stream)
        .noneMatch(StackGresContext.MANAGED_KEY::equals)) {
      LOGGER.info("Owning resource {} of kind {}",
          deployedResource.getMetadata().getName(),
          deployedResource.getKind());
      HasMetadata ownedResourceWithBeforeFirstApply = getClient()
          .serverSideApply(new PatchContext.Builder()
              .withFieldManager(ResourceWriter.STACKGRES_FIELD_MANAGER)
              .withForce(true)
              .build(), deployedResource);
      Optional.ofNullable(ownedResourceWithBeforeFirstApply)
          .map(HasMetadata::getMetadata)
          .map(ObjectMeta::getManagedFields)
          .stream()
          .flatMap(List::stream)
          .filter(managedFieldsEntry -> Objects.equals(
              managedFieldsEntry.getManager(), "before-first-apply"))
          .collect(Collectors.toList())
          .forEach(ownedResourceWithBeforeFirstApply
              .getMetadata().getManagedFields()::remove);
      HasMetadata ownedResourceWithoutBeforeFirstApply =
          getResourceOperation(getClient(), ownedResourceWithBeforeFirstApply)
          .inNamespace(ownedResourceWithBeforeFirstApply
              .getMetadata().getNamespace())
          .withName(ownedResourceWithBeforeFirstApply
              .getMetadata().getName())
          .lockResourceVersion(ownedResourceWithBeforeFirstApply
              .getMetadata().getResourceVersion())
          .replace(ownedResourceWithBeforeFirstApply);
      return ownedResourceWithoutBeforeFirstApply;
    }
    return deployedResource;
  }

  @SuppressWarnings("unchecked")
  private <M extends HasMetadata> MixedOperation<M, ? extends KubernetesResourceList<M>,
      ? extends Resource<M>> getResourceOperation(
      @NotNull KubernetesClient client, @NotNull M resource) {
    return (MixedOperation<M, ? extends KubernetesResourceList<M>, ? extends Resource<M>>) Optional
        .ofNullable(getResourceOperations(resource))
        .map(function -> function.apply(client))
        .orElseThrow(() -> new RuntimeException("Resource of type " + resource.getKind()
            + " is not configured"));
  }

  private <M extends HasMetadata> Function<KubernetesClient, MixedOperation<? extends HasMetadata,
      ? extends KubernetesResourceList<? extends HasMetadata>,
      ? extends Resource<? extends HasMetadata>>> getResourceOperations(M resource) {
    return ImmutableMap.<Class<? extends HasMetadata>,
        Function<KubernetesClient, MixedOperation<? extends HasMetadata,
            ? extends KubernetesResourceList<? extends HasMetadata>,
                ? extends Resource<? extends HasMetadata>>>>builder()
        .putAll(getInNamepspaceResourceOperations())
        .putAll(getAnyNamespaceResourceOperations())
        .build().get(resource.getClass());
  }

}
