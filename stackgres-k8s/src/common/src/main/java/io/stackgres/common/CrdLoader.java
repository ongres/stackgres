/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.io.Closer;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.stackgres.common.crd.CommonDefinition;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

public class CrdLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrdLoader.class);

  private final Yaml yamlParser;
  private final YAMLMapper yamlMapper;

  public CrdLoader(YAMLMapper yamlMapper) {
    final LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setMaxAliasesForCollections(100);
    this.yamlParser = new Yaml(loaderOptions);
    this.yamlMapper = yamlMapper;
  }

  public List<CustomResourceDefinition> scanCrds() {
    return streamCrdsFilenames()
        .map(this::readCrd)
        .toList();
  }

  public CustomResourceDefinition getCrd(String kind) {
    return streamCrdsFilenames()
        .filter(fileName -> fileName.equals(kind + ".yaml"))
        .findFirst()
        .map(this::readCrd)
        .orElseThrow(() -> new RuntimeException("CRD " + kind + " was not found"));
  }

  private Stream<String> streamCrdsFilenames() {
    Closer closer = Closer.create();
    try {
      InputStream is = closer.register(
          CommonDefinition.class.getResourceAsStream("/crds/index.txt"));
      InputStreamReader isr = closer.register(new InputStreamReader(is, StandardCharsets.UTF_8));
      BufferedReader br = closer.register(new BufferedReader(isr));
      return br.lines().onClose(Unchecked.runnable(closer::close));
    } catch (Exception ex) {
      Unchecked.runnable(closer::close).run();
      throw new RuntimeException(ex);
    }
  }

  public CustomResourceDefinition readCrd(String crdFilename) {
    LOGGER.debug("Read CRD {}", crdFilename);
    try (InputStream resourceAsStream = CommonDefinition.class.getResourceAsStream(
        "/crds/" + crdFilename)) {
      Object value = yamlParser.load(resourceAsStream);
      return yamlMapper
          .treeToValue(yamlMapper.valueToTree(value), CustomResourceDefinition.class);
    } catch (IOException ex) {
      throw new RuntimeException("Error while reading /crds/" + crdFilename, ex);
    }
  }

}
