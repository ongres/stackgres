/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import org.yaml.snakeyaml.LoaderOptions;

@SuppressWarnings("deprecation")
@Singleton
@Unremovable
public class YamlMapperProvider implements Supplier<YAMLMapper> {

  private static final YAMLMapper YAML_MAPPER = YAMLMapper
      .builder(
          YAMLFactory.builder()
          .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
          .loaderOptions(yamlLoaderOptions())
          .build())
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .disable(Feature.WRITE_DOC_START_MARKER)
      .enable(Feature.USE_NATIVE_OBJECT_ID)
      .build();

  private static LoaderOptions yamlLoaderOptions() {
    final LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setMaxAliasesForCollections(100);
    return loaderOptions;
  }

  static {
    YAML_MAPPER
        .registerModules(Serialization.UNMATCHED_FIELD_TYPE_MODULE)
        .configOverride(ArrayNode.class).setMergeable(false);
  }

  @Override
  public YAMLMapper get() {
    return YAML_MAPPER;
  }

}
