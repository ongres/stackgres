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
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.CommonDefinition;
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

  protected static CustomResourceDefinition readCrd(String crdFilename) {
    LOGGER.debug("Read CRD {}", crdFilename);
    try (InputStream resourceAsStream = CommonDefinition.class.getResourceAsStream(
        "/crds/" + crdFilename)) {
      return YAML_MAPPER
          .readValue(resourceAsStream, CustomResourceDefinition.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static List<String> getCrdsFilenames() {
    try (InputStream is = CommonDefinition.class.getResourceAsStream("/crds/index.txt");
        InputStreamReader isr = new InputStreamReader(is,
            StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)) {
      return br.lines().collect(Collectors.toUnmodifiableList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public List<CustomResourceDefinition> scanDefinitions() {
    return getCrdsFilenames()
        .stream()
        .map(CrdLoaderImpl::readCrd)
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
}
