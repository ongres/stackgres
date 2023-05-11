/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.stackgres.common.CrdLoader;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrUpdater.class);

  private final CrdLoader crdLoader;
  private final KubernetesClient client;

  public CrUpdater(CrdLoader crdLoader, KubernetesClient client) {
    this.crdLoader = crdLoader;
    this.client = client;
  }

  public void updateExistingCustomResources() {
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
    client.genericKubernetesResources(context)
        .inAnyNamespace()
        .list()
        .getItems()
        .stream()
        .forEach(resource -> KubernetesClientUtil
            .retryOnConflict(() -> {
              var currentResource = client.genericKubernetesResources(context)
                  .inNamespace(resource.getMetadata().getNamespace())
                  .withName(resource.getMetadata().getName())
                  .get();
              if (currentResource != null) {
                client.genericKubernetesResources(context)
                    .resource(currentResource)
                    .lockResourceVersion(currentResource.getMetadata().getResourceVersion())
                    .replace();
              }
            }));
  }

}
