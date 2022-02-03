/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static io.stackgres.operator.validation.CrdMatchTestHelper.getCustomResourceClass;
import static io.stackgres.operator.validation.CrdMatchTestHelper.getDefinition;
import static io.stackgres.operator.validation.CrdMatchTestHelper.withEveryYaml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import org.junit.jupiter.api.Test;

@SuppressWarnings("rawtypes")
class CrdMatchTest {

  private static final String CRD_VERSION = CommonDefinition.VERSION;

  private static final String CRD_GROUP = CommonDefinition.GROUP;

  @Test
  void apiVersion_ShouldMatchConfiguredVersion() throws IOException {
    CrdMatchTestHelper.withEveryYaml(crdTree -> {
      Class<? extends CustomResource> clazz = getCustomResourceClass(crdTree);
      String apiVersion = HasMetadata.getApiVersion(clazz);

      JsonNode crdInstallVersions = crdTree.get("spec").get("versions");
      String group = crdTree.get("spec").get("group").asText();

      var matchingSchema = StreamSupport.stream(
              Spliterators.spliteratorUnknownSize(
                  crdInstallVersions.elements(),
                  Spliterator.ORDERED),
              false)
          .filter(crdInstallVersion -> {
            String version = crdInstallVersion.get("name").asText();
            return Objects.equals(group + "/" + version, apiVersion);
          })
          .findAny();

      assertTrue(matchingSchema.isPresent(), "Kind : " + HasMetadata.getKind(clazz));

    });
  }

  @Test
  void crdVersion_ShouldMatchConfiguredVersion() throws IOException {
    withEveryYaml(crdTree -> {
      if (Objects.equals(
          crdTree.get("spec").get("names").get("kind").asText(),
          StackGresObjectStorage.KIND)) {
        /*
         * Skipping this test because the SGObjectStorage we only have v1beta1 version
         * at the moment
         */
        return;
      }

      if (Objects.equals(
          crdTree.get("spec").get("names").get("kind").asText(),
          StackGresCluster.KIND)) {
        /*
         * Skipping this test because the SGCluster is at v2beta1 version
         * at the moment
         */
        return;
      }
      JsonNode crdInstallVersions = crdTree.get("spec").get("versions");
      crdInstallVersions.elements();
      Class<? extends CustomResource> clazz = getCustomResourceClass(crdTree);

      boolean isThereASchemaThatMatches = StreamSupport.stream(
              Spliterators.spliteratorUnknownSize(
                  crdInstallVersions.elements(),
                  Spliterator.ORDERED),
              false)
          .anyMatch(crdInstallVersion -> Objects.equals(
                  CRD_VERSION,
                  crdInstallVersion.get("name").asText()
              ) && Objects.equals(
                  CRD_VERSION,
                  HasMetadata.getVersion(clazz))
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

}
