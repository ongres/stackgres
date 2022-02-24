/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;

public interface JsonPatchMutationPipeline<T> {

  ObjectMapper JSON_MAPPER = JsonMapper.builder()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .build();

  Optional<String> mutate(T review);

  default String join(List<JsonPatchOperation> operations) {

    JsonPatch patch = new JsonPatch(operations);

    try {
      return JSON_MAPPER.writeValueAsString(patch);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("error while processing json path", e);
    }

  }
}
