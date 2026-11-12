/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Write a value as a YAML scalar in a single line, quoted and escaped so that it can be placed as
 * is in any YAML document, as the value of a key in a flow mapping too.
 */
@Singleton
public class YamlScalarWriter {

  private final ObjectWriter singleLineYamlWriter;

  @Inject
  public YamlScalarWriter(YamlMapperProvider yamlMapperProvider) {
    final YAMLMapper singleLineYamlMapper = yamlMapperProvider.get().copy();
    ((YAMLFactory) singleLineYamlMapper.getFactory())
        .disable(YAMLGenerator.Feature.SPLIT_LINES);
    this.singleLineYamlWriter = singleLineYamlMapper.writer();
  }

  public String write(String value) {
    try {
      return singleLineYamlWriter.writeValueAsString(value).strip();
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }

}
