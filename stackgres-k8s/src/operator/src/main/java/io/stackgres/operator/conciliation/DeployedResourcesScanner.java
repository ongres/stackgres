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

public abstract class DeployedResourcesScanner<T extends CustomResource<?, ?>> {

  public List<HasMetadata> getDeployedResources(T config) {
    final String kind = HasMetadata.getKind(config.getClass());
    final Map<String, String> genericLabels = getGenericLabels(config);
    final Map<String, String> crossNamespaceLabels = getCrossNamespaceLabels(config);

    KubernetesClient client = getClient();

    Stream<HasMetadata> inNamespace = getInNamepspaceResourceOperations()
        .values()
        .stream()
        .filter(op -> !genericLabels.isEmpty())
        .flatMap(emptyOnNotFound(op -> op.apply(client)
            .inNamespace(config.getMetadata().getNamespace())
            .withLabels(genericLabels)
            .list()
            .getItems()
            .stream()));

    Stream<HasMetadata> inAnyNamespace = getInAnyNamespaceResourceOperations(config)
        .values()
        .stream()
        .filter(op -> !crossNamespaceLabels.isEmpty())
        .flatMap(emptyOnNotFound(op -> op.apply(client)
            .inAnyNamespace()
            .withLabels(crossNamespaceLabels)
            .list()
            .getItems()
            .stream()));

    List<HasMetadata> deployedResources = Seq.seq(inNamespace)
        .filter(resource -> resource.getMetadata().getOwnerReferences()
            .stream().anyMatch(ownerReference -> ownerReference.getKind().equals(kind)
                && ownerReference.getName().equals(config.getMetadata().getName())
                && ownerReference.getUid().equals(config.getMetadata().getUid())))
        .append(inAnyNamespace)
        .toList();

    return deployedResources;
  }

  private <P, R> Function<P, Stream<R>> emptyOnNotFound(Function<P, Stream<R>> function) {
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
