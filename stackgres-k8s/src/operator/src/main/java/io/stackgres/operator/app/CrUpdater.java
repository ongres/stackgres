/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.stackgres.common.CrdLoader;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CrUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrUpdater.class);

  private final List<String> allowedNamespaces = OperatorProperty.getAllowedNamespaces();

  private final KubernetesClient client;
  private final CrdLoader crdLoader;

  @Inject
  public CrUpdater(
      KubernetesClient client,
      YamlMapperProvider yamlMapperProvider) {
    this.client = client;
    this.crdLoader = new CrdLoader(yamlMapperProvider.get());
  }

  public void updateExistingCustomResources() {
    LOGGER.info("Updating existing custom resources");
    crdLoader.scanCrds()
        .stream()
        .forEach(installedCrd -> {
          LOGGER.info("Patching existing custom resources to apply defaults for CRD {}",
              installedCrd.getSpec().getNames().getKind());
          updateExistingCustomResources(installedCrd);
          LOGGER.info("Existing custom resources for CRD {}. Patched",
              installedCrd.getSpec().getNames().getKind());
        });
  }

  private void updateExistingCustomResources(
      @NotNull CustomResourceDefinition customResourceDefinition) {
    ResourceDefinitionContext context = new ResourceDefinitionContext.Builder()
        .withGroup(customResourceDefinition.getSpec().getGroup())
        .withVersion(customResourceDefinition.getSpec().getVersions().stream()
            .filter(CustomResourceDefinitionVersion::getStorage)
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "Storage is not set for any version of CRD "
                    + customResourceDefinition.getSpec().getNames().getKind()))
            .getName())
        .withNamespaced(customResourceDefinition.getSpec().getScope().equals("Namespaced"))
        .withPlural(customResourceDefinition.getSpec().getNames().getPlural())
        .withKind(customResourceDefinition.getSpec().getNames().getKind())
        .build();
    listCrdResources(customResourceDefinition)
        .stream()
        .forEach(resource -> KubernetesClientUtil
            .retryOnError(() -> KubernetesClientUtil
                .retryOnConflict(() -> {
                  var currentResource = client.genericKubernetesResources(context)
                      .inNamespace(resource.getMetadata().getNamespace())
                      .withName(resource.getMetadata().getName())
                      .get();
                  if (currentResource != null) {
                    client.genericKubernetesResources(context)
                        .resource(currentResource)
                        .lockResourceVersion(currentResource.getMetadata().getResourceVersion())
                        .update();
                  }
                }), 5));
  }

  List<GenericKubernetesResource> listCrdResources(CustomResourceDefinition crd) {
    var genericKubernetesResources =
        client.genericKubernetesResources(CustomResourceDefinitionContext.fromCrd(crd));
    return Optional.of(allowedNamespaces)
        .filter(Predicate.not(List::isEmpty))
        .map(allowedNamespaces -> allowedNamespaces.stream()
            .flatMap(allowedNamespace -> Optional
                .ofNullable(genericKubernetesResources
                    .inNamespace(allowedNamespace)
                    .list()
                    .getItems()).stream())
            .reduce(Seq.<GenericKubernetesResource>of(), (seq, items) -> seq.append(items), (u, v) -> v)
            .toList())
        .orElseGet(() -> genericKubernetesResources
            .inAnyNamespace()
            .list()
            .getItems());
  }

}
