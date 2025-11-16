/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionNames;
import org.junit.jupiter.api.Test;

class CrdLoaderTest {

  static final File crdsFolder = getCrdsFolder();

  private YAMLMapper mapper = new YamlMapperProvider().get();

  private CrdLoader crdLoader = new CrdLoader(mapper);

  @Test
  void scanDefinitions() {
    List<CustomResourceDefinition> definitions = crdLoader.scanCrds();

    assertEquals(crdsFolder
        .list((file, name) -> name.endsWith(".yaml")).length, definitions.size());

    List<CustomResourceDefinition> customResourceDefinitions = crdLoader.scanCrds();

    definitions.forEach(def -> {
      var customResourceDefinition = customResourceDefinitions.stream()
          .filter(crd -> crd.getMetadata().getName().equals(def.getMetadata().getName()))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("There is no definition with name "
              + def.getMetadata().getName()));

      final CustomResourceDefinitionNames names = customResourceDefinition.getSpec().getNames();
      assertEquals(names.getKind(), def.getSpec().getNames().getKind());
      assertEquals(names.getSingular(), def.getSpec().getNames().getSingular());
    });
  }

  static File getCrdsFolder() {
    String projectPath = new File(new File("src").getAbsolutePath())
        .getParentFile().getParentFile().getParentFile().getAbsolutePath();

    return new File(projectPath + "/src/common/src/main/resources/crds");
  }

}
