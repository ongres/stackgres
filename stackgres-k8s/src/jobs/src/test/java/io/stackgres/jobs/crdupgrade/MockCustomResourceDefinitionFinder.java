/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.testutil.CrdUtils;
import org.jetbrains.annotations.NotNull;

public class MockCustomResourceDefinitionFinder
    implements ResourceFinder<CustomResourceDefinition>, CrdLoader {

  private final YAMLMapper yamlMapper = new YAMLMapper();

  private final File crdFolder = CrdUtils.getCrdsFolder();

  @Override
  public Optional<CustomResourceDefinition> findByName(String name) {
    File[] crdFiles = crdFolder.listFiles();

    return Arrays.stream(crdFiles)
        .filter(file -> file.getName().endsWith(".yaml"))
        .map(crdFile -> {
          try {
            return yamlMapper.readValue(crdFile, CustomResourceDefinition.class);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .filter(crd -> crd.getMetadata().getName().equals(name))
        .findFirst();
  }

  @Override
  public Optional<CustomResourceDefinition> findByNameAndNamespace(String name, String namespace) {
    return findByName(name);
  }

  @Override
  public CustomResourceDefinition load(String kind) {
    File file = crdFolder.listFiles(filename -> filename.getName().equals(kind + ".yaml"))[0];
    try {
      return yamlMapper.readValue(file, CustomResourceDefinition.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<CustomResourceDefinition> scanDefinitions() {
    return Arrays.stream(crdFolder.listFiles())
        .filter(file -> file.getName().endsWith(".yaml"))
        .map(file -> {
          try {
            return yamlMapper.readValue(file, CustomResourceDefinition.class);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }).collect(Collectors.toUnmodifiableList());
  }

  @Override
  public void updateExistingCustomResources(
      @NotNull CustomResourceDefinition customResourceDefinition) {
  }

}
