/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.testutil.CrdUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.reflections.Reflections;

@SuppressWarnings("rawtypes")
class CrdMatchTest {

  private static final String CRD_VERSION = CommonDefinition.VERSION;

  private static final String CRD_GROUP = CommonDefinition.GROUP;

  private static File[] crdFiles;

  private static Map<String, CustomResourceDefinition> definitionsByKind;

  private static Map<String, Class<? extends CustomResource>> classByKind;

  @BeforeAll
  static void beforeAll() throws Exception {
    crdFiles = CrdUtils.getCrdsFolder()
        .listFiles(file -> file.getName().endsWith(".yaml"));
    definitionsByKind = getCustomResourceClasses().stream()
        .map(clazz -> CustomResourceDefinitionContext.v1CRDFromCustomResourceType(clazz).build())
        .collect(Collectors.toMap(crd -> crd.getSpec().getNames().getKind(), Function.identity()));
    classByKind = getCustomResourceClasses().stream()
        .collect(Collectors.toMap(clazz -> HasMetadata.getKind(clazz), Function.identity()));
  }

  private static Set<Class<? extends CustomResource>> getCustomResourceClasses() {
    Reflections reflections = new Reflections("io.stackgres.common.crd");
    return reflections.getSubTypesOf(CustomResource.class);
  }

  private static void withEveryYaml(Consumer<JsonNode> crdDefinition) throws IOException {
    YAMLMapper yamlMapper = new YamlMapperProvider().yamlMapper();
    for (File crd : crdFiles) {
      JsonNode crdTree = yamlMapper.readTree(crd);
      crdDefinition.accept(crdTree);
    }
  }

  private static CustomResourceDefinition getDefinition(JsonNode crdTree) {
    String declaredKind = crdTree.get("spec").get("names").get("kind").asText();
    return Optional.ofNullable(definitionsByKind.get(declaredKind))
        .orElseThrow(() -> new AssertionFailedError("CustomResourceDefinition "
            + declaredKind + " does not exists. Available kinds: " + definitionsByKind.keySet()));
  }

  private static Class<? extends CustomResource> getCustomResourceClass(JsonNode crdTree) {
    String declaredKind = crdTree.get("spec").get("names").get("kind").asText();
    return Optional.ofNullable(classByKind.get(declaredKind))
        .orElseThrow(() -> new AssertionFailedError("CustomResourceDefinition "
            + declaredKind + " does not exists. Available kinds: " + definitionsByKind.keySet()));
  }

  @Test
  void apiVersion_ShouldMatchConfiguredVersion() throws IOException {
    withEveryYaml(crdTree -> {
      Class<? extends CustomResource> clazz = getCustomResourceClass(crdTree);
      String apiVersion = HasMetadata.getApiVersion(clazz);

      JsonNode crdInstallVersions = crdTree.get("spec").get("versions");
      String group = crdTree.get("spec").get("group").asText();

      var matchingSchema = StreamSupport.stream(
              Spliterators.spliteratorUnknownSize(
                  crdInstallVersions.elements(),
                  Spliterator.ORDERED
              ),
              false)
          .filter(crdInstallVersion -> {
                String version = crdInstallVersion.get("name").asText();
                return Objects.equals(group + "/" + version, apiVersion);
              }
          )
          .findAny();

      assertTrue(matchingSchema.isPresent(), "Kind : " + HasMetadata.getKind(clazz));

    });
  }

  @Test
  void crdVersion_ShouldMatchConfiguredVersion() throws IOException {
    withEveryYaml(crdTree -> {
      JsonNode crdInstallVersions = crdTree.get("spec").get("versions");
      crdInstallVersions.elements();

      Class<? extends CustomResource> clazz = getCustomResourceClass(crdTree);

      boolean isThereASchemaThatMatches = StreamSupport.stream(
              Spliterators.spliteratorUnknownSize(
                  crdInstallVersions.elements(),
                  Spliterator.ORDERED
              ),
              false)
          .anyMatch(crdInstallVersion ->
              Objects.equals(CRD_VERSION, crdInstallVersion.get("name").asText())
                  && Objects.equals(CRD_VERSION, HasMetadata.getVersion(clazz))
          );

      assertTrue(isThereASchemaThatMatches,
          "At least one schema should have the version " + CRD_VERSION);
    });
  }

  @Test
  void crdVersion_ShouldMatchConfiguredGroup() throws IOException {
    withEveryYaml(crdTree -> {
      String yamlGroup = crdTree.get("spec").get("group").asText();
      assertEquals(CRD_GROUP, yamlGroup);

      Class<? extends CustomResource> clazz = getCustomResourceClass(crdTree);
      String group = HasMetadata.getGroup(clazz);
      assertEquals(yamlGroup, group, "Kind : " + HasMetadata.getKind(clazz));
    });
  }

  @Test
  void customResourcesYamlSingular_shouldMatchWithSingularInJavaDefinition() throws IOException {
    withEveryYaml(crdTree -> {
      JsonNode crdNames = crdTree.get("spec").get("names");
      String declaredSingular = crdNames.get("singular").asText();
      CustomResourceDefinition definition = getDefinition(crdTree);
      assertEquals(declaredSingular, definition.getSpec().getNames().getSingular());

      Class<? extends CustomResource> clazz = getCustomResourceClass(crdTree);
      String singular = HasMetadata.getSingular(clazz);
      assertEquals(declaredSingular, singular, "Kind : " + HasMetadata.getKind(clazz));
    });
  }

  @Test
  void customResourcesYamlDefinitionsPlural_ShouldMatchWithPluralInJavaDefinition()
      throws IOException {
    withEveryYaml(crdTree -> {
      JsonNode crdNames = crdTree.get("spec").get("names");
      String declaredPlural = crdNames.get("plural").asText();
      CustomResourceDefinition definition = getDefinition(crdTree);
      assertEquals(declaredPlural, definition.getSpec().getNames().getPlural());

      Class<? extends CustomResource> clazz = getCustomResourceClass(crdTree);
      String plural = HasMetadata.getPlural(clazz);
      assertEquals(declaredPlural, plural, "Kind : " + HasMetadata.getKind(clazz));
    });
  }

  @Test
  void customResourcesYamlMetadataName_ShouldMatchWithNameInJavaDefinition() throws IOException {
    withEveryYaml(crdTree -> {
      JsonNode metadataName = crdTree.get("metadata").get("name");
      CustomResourceDefinition definition = getDefinition(crdTree);
      assertEquals(metadataName.asText(), definition.getMetadata().getName());

      Class<? extends CustomResource> clazz = getCustomResourceClass(crdTree);
      String name = CustomResource.getCRDName(clazz);
      assertEquals(metadataName.asText(), name, "Kind : " + HasMetadata.getKind(clazz));
    });
  }

  @Test
  void customResourcesYamlNamespaced_ShouldMatchWithNamespacedInJavaDefinition()
      throws IOException {
    withEveryYaml(crdTree -> {
      JsonNode metadataName = crdTree.get("spec").get("scope");
      CustomResourceDefinition definition = getDefinition(crdTree);
      assertEquals(metadataName.asText(), definition.getSpec().getScope());
    });
  }

}
