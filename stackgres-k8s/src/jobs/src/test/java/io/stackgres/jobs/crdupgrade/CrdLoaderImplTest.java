/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionNames;
import io.stackgres.testutil.CrdUtils;
import org.junit.jupiter.api.Test;

class CrdLoaderImplTest {

  private CrdLoaderImpl crdLoader = new CrdLoaderImpl(null);

  private File crdFolder = CrdUtils.getCrdsFolder();

  private YAMLMapper mapper = new YAMLMapper();

  @Test
  void scanDefinitions() {
    List<CustomResourceDefinition> definitions = crdLoader.scanDefinitions();

    assertEquals(crdFolder.list((file, name) -> name.endsWith(".yaml")).length, definitions.size());

    List<CustomResourceDefinition> customResourceDefinitions = Arrays
        .stream(crdFolder.listFiles((file, name) -> name.endsWith(".yaml")))
        .map(file -> {
          try {
            return mapper
                .readValue(file, CustomResourceDefinition.class);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toList());

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

}
