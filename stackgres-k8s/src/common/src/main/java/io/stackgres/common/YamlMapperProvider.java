/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class YamlMapperProvider {

  private static final YAMLMapper YAML_MAPPER = createYamlMapper();

  private static YAMLMapper createYamlMapper() {
    YAMLMapper yamlMapper = new YAMLMapper();
    yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    yamlMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    return yamlMapper;
  }

  public YAMLMapper yamlMapper() {
    return YAML_MAPPER;
  }

}
