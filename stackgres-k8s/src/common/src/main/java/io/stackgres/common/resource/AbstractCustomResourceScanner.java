/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import static io.stackgres.common.kubernetesclient.KubernetesClientUtil.listOrEmptyOnForbidden;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.Seq;

public abstract class AbstractCustomResourceScanner<T extends CustomResource<?, ?>,
        L extends DefaultKubernetesResourceList<T>>
    implements CustomResourceScanner<T> {

  private final List<String> allowedNamespaces = OperatorProperty.getAllowedNamespaces();
  private final boolean clusterRoleDisabled = OperatorProperty.CLUSTER_ROLE_DISABLED.getBoolean();

  private final KubernetesClient client;

  private final Class<T> customResourceClass;
  private final Class<L> customResourceListClass;

  protected AbstractCustomResourceScanner(KubernetesClient client,
      Class<T> customResourceClass,
      Class<L> customResourceListClass) {
    this.client = client;
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
  }

  public AbstractCustomResourceScanner() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
    this.customResourceClass = null;
    this.customResourceListClass = null;
  }

  @Override
  public Optional<List<T>> findResources() {
    String crdName = CustomResource.getCRDName(customResourceClass);
    return Optional.of(allowedNamespaces)
        .filter(Predicate.not(List::isEmpty))
        .map(allowedNamespaces -> allowedNamespaces.stream()
            .flatMap(allowedNamespace -> Optional.of(clusterRoleDisabled)
                .filter(clusterRoleDisabled -> clusterRoleDisabled)
                .or(() -> Optional
                    .of(client.apiextensions().v1().customResourceDefinitions()
                    .withName(crdName)
                    .get() != null))
                .filter(crdFound -> crdFound)
                .map(crdFound -> listOrEmptyOnForbidden(() -> client
                    .resources(customResourceClass, customResourceListClass)
                    .inNamespace(allowedNamespace)
                    .list()
                    .getItems()))
                .stream())
            .reduce(Seq.<T>of(), (seq, items) -> seq.append(items), (u, v) -> v)
            .toList())
        .or(() -> Optional.of(clusterRoleDisabled)
            .filter(clusterRoleDisabled -> clusterRoleDisabled)
            .or(() -> Optional
                .of(client.apiextensions().v1().customResourceDefinitions()
                .withName(crdName)
                .get() != null))
            .filter(crdFound -> crdFound)
            .map(crdFound -> client.resources(customResourceClass, customResourceListClass)
                .inAnyNamespace()
                .list()
                .getItems()));
  }

  @Override
  public Optional<List<T>> findResources(@Nullable String namespace) {
    String crdName = CustomResource.getCRDName(customResourceClass);
    return Optional.of(clusterRoleDisabled)
        .filter(clusterRoleDisabled -> clusterRoleDisabled)
        .or(() -> Optional
            .of(client.apiextensions().v1().customResourceDefinitions()
            .withName(crdName)
            .get() != null))
        .filter(crdFound -> crdFound)
        .map(crdFound -> client.resources(customResourceClass, customResourceListClass)
            .inNamespace(namespace)
            .list()
            .getItems());
  }

  @Override
  public List<T> getResources() {
    return Optional.of(allowedNamespaces)
        .filter(Predicate.not(List::isEmpty))
        .map(allowedNamespaces -> allowedNamespaces.stream()
            .flatMap(allowedNamespace -> Optional
                .ofNullable(client.resources(customResourceClass, customResourceListClass)
                    .inNamespace(allowedNamespace)
                    .list()
                    .getItems()).stream())
            .reduce(Seq.<T>of(), (seq, items) -> seq.append(items), (u, v) -> v)
            .toList())
        .orElseGet(() -> client.resources(customResourceClass, customResourceListClass)
            .inAnyNamespace()
            .list()
            .getItems());
  }

  @Override
  public List<T> getResources(@Nullable String namespace) {
    return client.resources(customResourceClass, customResourceListClass)
        .inNamespace(namespace)
        .list()
        .getItems();
  }

  @Override
  public @NotNull List<@NotNull T> getResourcesWithLabels(Map<String, String> labels) {
    return Optional.of(allowedNamespaces)
        .filter(Predicate.not(List::isEmpty))
        .map(allowedNamespaces -> allowedNamespaces.stream()
            .flatMap(allowedNamespace -> Optional
                .ofNullable(client.resources(customResourceClass, customResourceListClass)
                    .inNamespace(allowedNamespace)
                    .list()
                    .getItems()).stream())
            .reduce(Seq.<T>of(), (seq, items) -> seq.append(items), (u, v) -> v)
            .toList())
        .orElseGet(() -> client.resources(customResourceClass, customResourceListClass)
            .inAnyNamespace()
            .withLabels(labels)
            .list()
            .getItems());
  }

  @Override
  public @NotNull List<@NotNull T> getResourcesWithLabels(
      String namespace, Map<String, String> labels) {
    return client.resources(customResourceClass, customResourceListClass)
        .inNamespace(namespace)
        .withLabels(labels)
        .list()
        .getItems();
  }

}
