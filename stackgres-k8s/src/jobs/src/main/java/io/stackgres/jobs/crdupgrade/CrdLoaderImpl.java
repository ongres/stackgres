/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.jobs.common.KubernetesClientFactory;
import io.stackgres.jobs.common.StackGresCustomResourceDefinition;

public class CrdLoaderImpl implements CrdLoader {

  private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

  private final KubernetesClientFactory kubernetesClientFactory;

  public CrdLoaderImpl(KubernetesClientFactory kubernetesClientFactory) {
    this.kubernetesClientFactory = kubernetesClientFactory;
  }

  protected static CustomResourceDefinition readCrd(String crdFilename) {

    try (InputStream resourceAsStream = CrdLoaderImpl.class.getResourceAsStream(
        "/crds/" + crdFilename)) {
      return YAML_MAPPER
          .readValue(resourceAsStream, CustomResourceDefinition.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static StackGresCustomResourceDefinition toStackGresDefinition(
      CustomResourceDefinition crd) {
    StackGresCustomResourceDefinition stackGresCustomResourceDefinition
        = new StackGresCustomResourceDefinition();
    stackGresCustomResourceDefinition.setKind(crd.getSpec().getNames().getKind());
    stackGresCustomResourceDefinition.setName(crd.getMetadata().getName());
    stackGresCustomResourceDefinition.setSingular(crd.getSpec().getNames().getSingular());
    return stackGresCustomResourceDefinition;
  }

  protected static List<String> getCrdsFilenames() {
    try (InputStream is = CrdLoaderImpl.class.getResourceAsStream("/crds/index.txt");
         InputStreamReader isr = new InputStreamReader(is,
             StandardCharsets.UTF_8);
         BufferedReader br = new BufferedReader(isr)) {
      return br.lines().collect(Collectors.toUnmodifiableList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public List<StackGresCustomResourceDefinition> scanDefinitions() {

    return getCrdsFilenames()
        .stream()
        .map(CrdLoaderImpl::readCrd)
        .map(CrdLoaderImpl::toStackGresDefinition)
        .collect(Collectors.toUnmodifiableList());

  }

  @Override
  public CustomResourceDefinition load(String kind) {
    try (KubernetesClient client = kubernetesClientFactory.create()) {
      return client.apiextensions().v1().customResourceDefinitions().load(ClassLoader
          .getSystemResourceAsStream("crds/" + kind + ".yaml"))
          .get();
    }
  }
}
