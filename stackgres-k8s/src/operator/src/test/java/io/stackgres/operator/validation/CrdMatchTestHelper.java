/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.testutil.CrdUtils;
import org.opentest4j.AssertionFailedError;
import org.reflections.Reflections;

@SuppressWarnings("rawtypes")
public class CrdMatchTestHelper {

  private static File[] crdFiles;

  private static Map<String, CustomResourceDefinition> definitionsByKind;

  private static Map<String, Class<? extends CustomResource>> classByKind;

  public CrdMatchTestHelper() {
    crdFiles = CrdUtils.getCrdsFolder()
        .listFiles(file -> file.getName().endsWith(".yaml"));
    definitionsByKind = getCustomResourceClasses().stream()
        .map(clazz -> CustomResourceDefinitionContext.v1CRDFromCustomResourceType(clazz).build())
        .collect(Collectors.toMap(crd -> crd.getSpec().getNames().getKind(), Function.identity()));
    classByKind = getCustomResourceClasses().stream()
        .collect(Collectors.toMap(clazz -> HasMetadata.getKind(clazz), Function.identity()));
  }

  protected Set<Class<? extends CustomResource>> getCustomResourceClasses() {
    Reflections reflections = new Reflections("io.stackgres.common.crd");
    return reflections.getSubTypesOf(CustomResource.class);
  }

  protected void withEveryYaml(Consumer<JsonNode> crdDefinition) throws IOException {
    YAMLMapper yamlMapper = new YamlMapperProvider().yamlMapper();
    for (File crd : crdFiles) {
      if (isOneOfThose(crd.getName())) {
        JsonNode crdTree = yamlMapper.readTree(crd);
        crdDefinition.accept(crdTree);
      }
    }
  }

  private boolean isOneOfThose(String name) {
    if (getSelectedCrds().isEmpty()) {
      return true;
    }

    return getSelectedCrds().contains(name);
  }

  protected List<String> getSelectedCrds() {
    return new ArrayList<String>();
  }

  protected CustomResourceDefinition getDefinition(JsonNode crdTree) {
    String declaredKind = crdTree.get("spec").get("names").get("kind").asText();
    return Optional.ofNullable(definitionsByKind.get(declaredKind))
        .orElseThrow(() -> new AssertionFailedError("CustomResourceDefinition "
            + declaredKind + " does not exists. Available kinds: " + definitionsByKind.keySet()));
  }

  protected Class<? extends CustomResource> getCustomResourceClass(JsonNode crdTree) {
    String declaredKind = crdTree.get("spec").get("names").get("kind").asText();
    return Optional.ofNullable(classByKind.get(declaredKind))
        .orElseThrow(() -> new AssertionFailedError("CustomResourceDefinition "
            + declaredKind + " does not exists. Available kinds: " + definitionsByKind.keySet()));
  }
}
