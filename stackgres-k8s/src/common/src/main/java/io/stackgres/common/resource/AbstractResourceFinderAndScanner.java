/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import static io.stackgres.common.kubernetesclient.KubernetesClientUtil.listOrEmptyOnForbiddenOrNotFound;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import org.jooq.lambda.Seq;

public abstract class AbstractResourceFinderAndScanner<T extends HasMetadata>
    implements ResourceFinder<T>, ResourceScanner<T> {

  private final List<String> allowedNamespaces = OperatorProperty.getAllowedNamespaces();

  private final KubernetesClient client;

  public AbstractResourceFinderAndScanner(KubernetesClient client) {
    this.client = client;
  }

  public AbstractResourceFinderAndScanner() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
  }

  @Override
  public Optional<T> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<T> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(getOperation(client)
        .inNamespace(namespace)
        .withName(name)
        .get());
  }

  @Override
  public List<T> getResources() {
    return Optional.of(allowedNamespaces)
        .filter(Predicate.not(List::isEmpty))
        .map(allowedNamespaces -> allowedNamespaces.stream()
            .map(allowedNamespace -> listOrEmptyOnForbiddenOrNotFound(() -> getOperation(client)
                .inNamespace(allowedNamespace)
                .list()
                .getItems()))
            .flatMap(List::stream)
            .reduce(Seq.<T>of(), (seq, items) -> seq.append(items), (u, v) -> v)
            .toList())
        .orElseGet(() -> getOperation(client)
            .inAnyNamespace()
            .list()
            .getItems());
  }

  @Override
  public List<T> getResourcesWithLabels(Map<String, String> labels) {
    return Optional.of(allowedNamespaces)
        .filter(Predicate.not(List::isEmpty))
        .map(allowedNamespaces -> allowedNamespaces.stream()
            .map(allowedNamespace -> listOrEmptyOnForbiddenOrNotFound(() -> getOperation(client)
                .inNamespace(allowedNamespace)
                .list()
                .getItems()))
            .flatMap(List::stream)
            .reduce(Seq.<T>of(), (seq, items) -> seq.append(items), (u, v) -> v)
            .toList())
        .orElseGet(() -> getOperation(client)
            .inAnyNamespace()
            .withLabels(labels)
            .list()
            .getItems());
  }

  @Override
  public List<T> getResourcesInNamespace(String namespace) {
    return getOperation(client)
        .inNamespace(namespace)
        .list()
        .getItems();
  }

  @Override
  public List<T> getResourcesInNamespaceWithLabels(String namespace, Map<String, String> labels) {
    return getOperation(client)
        .inNamespace(namespace)
        .withLabels(labels)
        .list()
        .getItems();
  }

  protected abstract MixedOperation<T, ? extends KubernetesResourceList<T>, ? extends Resource<T>> getOperation(
      KubernetesClient client);

}
