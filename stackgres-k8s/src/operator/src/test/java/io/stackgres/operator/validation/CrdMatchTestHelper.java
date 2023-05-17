/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.stackgres.common.CrdLoader;
import io.stackgres.common.YamlMapperProvider;
import org.opentest4j.AssertionFailedError;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

@SuppressWarnings("rawtypes")
public class CrdMatchTestHelper {

  private static final Set<Class<? extends CustomResource>> SG_CR_CLASSES = scanCrdsClasses();

  private static Set<Class<? extends CustomResource>> scanCrdsClasses() {
    Reflections reflections = new Reflections(new ConfigurationBuilder()
        .forPackage("io.stackgres.common.crd")
        .setScanners(Scanners.SubTypes));
    return Set.copyOf(reflections.getSubTypesOf(CustomResource.class));
  }

  public static int getMaxLengthResourceNameFrom(String kind)
      throws JsonProcessingException, IOException {
    var yamlMapper = new YamlMapperProvider().get();
    JsonNode crdTree = yamlMapper.valueToTree(new CrdLoader(yamlMapper).getCrd(kind));
    JsonNode maxLengthResourceName = extractMetadataMaxLengthResourceName(crdTree);
    return Optional.of(maxLengthResourceName.intValue()).orElse(null);
  }

  private static JsonNode extractMetadataMaxLengthResourceName(JsonNode crdTree) {
    return crdTree.get("spec").get("versions").get(0).get("schema").get("openAPIV3Schema")
        .get("properties").get("metadata")
        .get("properties").get("name").get("maxLength");
  }

  private static Set<Class<? extends CustomResource>> getCustomResourceClasses() {
    return SG_CR_CLASSES;
  }

  public static void withEveryYaml(Consumer<JsonNode> crdDefinition) throws IOException {
    var yamlMapper = new YamlMapperProvider().get();
    for (var crd : new CrdLoader(yamlMapper).scanCrds()) {
      JsonNode crdTree = yamlMapper.valueToTree(crd);
      crdDefinition.accept(crdTree);
    }
  }

  private static Map<String, Class<? extends CustomResource>> getClassByKind() {
    return getCustomResourceClasses().stream()
        .collect(Collectors.toMap(clazz -> HasMetadata.getKind(clazz), Function.identity()));
  }

  private static Map<String, CustomResourceDefinition> getDefinitionByKind() {
    return getCustomResourceClasses().stream()
        .map(clazz -> CustomResourceDefinitionContext.v1CRDFromCustomResourceType(clazz).build())
        .collect(Collectors.toMap(crd -> crd.getSpec().getNames().getKind(), Function.identity()));
  }

  protected static CustomResourceDefinition getDefinition(JsonNode crdTree) {
    String declaredKind = crdTree.get("spec").get("names").get("kind").asText();
    return Optional.ofNullable(getDefinitionByKind().get(declaredKind))
        .orElseThrow(() -> new AssertionFailedError("CustomResourceDefinition "
            + declaredKind + " does not exists. Available kinds: "
            + getDefinitionByKind().keySet()));
  }

  protected static Class<? extends CustomResource> getCustomResourceClass(JsonNode crdTree) {
    String declaredKind = crdTree.get("spec").get("names").get("kind").asText();
    return Optional.ofNullable(getClassByKind().get(declaredKind))
        .orElseThrow(() -> new AssertionFailedError("CustomResourceDefinition "
            + declaredKind + " does not exists. Available kinds: "
            + getDefinitionByKind().keySet()));
  }
}
