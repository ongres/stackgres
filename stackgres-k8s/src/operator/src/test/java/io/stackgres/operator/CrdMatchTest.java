/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.stackgres.operator.app.YamlMapperProvider;
import io.stackgres.operator.common.ConfigLoader;
import io.stackgres.operator.common.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CrdMatchTest {

  private static final ConfigLoader configLoader = new ConfigLoader();

  private static String crdPomVersion;

  private static File[] crdFiles;

  @BeforeAll
  static void beforeAll() {

    crdPomVersion = configLoader.getProperty(ConfigProperty.CRD_VERSION)
        .orElseThrow(() -> new IllegalStateException("Crd version not configured"));

    String projectPath = new File(new File("src").getAbsolutePath())
        .getParentFile().getParentFile().getParentFile().getAbsolutePath();

    File crdFolder = new File(projectPath + "/install/helm/stackgres-operator/crds");

    crdFiles = crdFolder.listFiles();

  }

  @Test
  void crdVersion_ShouldMatchConfiguredVersion() throws IOException {

    YAMLMapper yamlMapper = new YamlMapperProvider().yamlMapper();

    for (File crd: crdFiles){
      JsonNode crdTree = yamlMapper.readTree(crd);

      JsonNode crdInstallVersions = crdTree.get("spec").get("versions");

      for (JsonNode crdInstallVersion: crdInstallVersions){
        assertEquals(crdPomVersion, crdInstallVersion.get("name").asText());

      }




    }

  }

}
