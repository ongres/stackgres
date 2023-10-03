/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
@SuppressFBWarnings(value = "EQ_COMPARETO_USE_OBJECT_EQUALS",
    justification = "Not required")
public class JsonMapperCustomizer implements ObjectMapperCustomizer {

  /**
   * Used to configure Quarkus provided ObjectMapper.
   */
  @Override
  public void customize(ObjectMapper objectMapper) {
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
  }

}
