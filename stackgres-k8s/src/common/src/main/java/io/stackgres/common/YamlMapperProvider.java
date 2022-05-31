/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.function.Supplier;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.arc.Unremovable;

@Singleton
@Unremovable
public class YamlMapperProvider implements Supplier<YAMLMapper> {

  private static final YAMLMapper YAML_MAPPER = YAMLMapper.builder(new YAMLFactory()
      .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .disable(Feature.WRITE_DOC_START_MARKER)
      .build();

  static {
    YAML_MAPPER.registerModules(Serialization.UNMATCHED_FIELD_TYPE_MODULE);
  }

  @Override
  public YAMLMapper get() {
    return YAML_MAPPER;
  }

}
