/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrdLoaderImpl implements CrdLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrdLoaderImpl.class);

  private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

  private final KubernetesClient client;

  public CrdLoaderImpl(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public List<CustomResourceDefinition> scanDefinitions() {
    return getCrdsFilenames()
        .stream()
        .map(this::readCrd)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public CustomResourceDefinition load(@NotNull String kind) {
    LOGGER.debug("Loading CRD {}", kind);
    try (InputStream is = CommonDefinition.class.getResourceAsStream("/crds/" + kind + ".yaml")) {
      return client.apiextensions().v1().customResourceDefinitions()
          .load(is)
          .get();
    } catch (IOException cause) {
      // Error on closing InputStream
      throw new UncheckedIOException(cause);
    }
  }

  @Override
  public void updateExistingCustomResources(
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

  private CustomResourceDefinition readCrd(String crdFilename) {
    LOGGER.debug("Read CRD {}", crdFilename);
    try (InputStream resourceAsStream = CommonDefinition.class.getResourceAsStream(
        "/crds/" + crdFilename)) {
      return YAML_MAPPER
          .readValue(resourceAsStream, CustomResourceDefinition.class);
    } catch (IOException ex) {
      throw new RuntimeException("Error while reading /crds/" + crdFilename, ex);
    }
  }

  private List<String> getCrdsFilenames() {
    try (InputStream is = CommonDefinition.class.getResourceAsStream("/crds/index.txt");
        InputStreamReader isr = new InputStreamReader(is,
            StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)) {
      return br.lines().collect(Collectors.toUnmodifiableList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
}
