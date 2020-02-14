/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;

public interface JsonPatchMutationPipeline<T> {

  ObjectMapper mapper = new ObjectMapper();

  Optional<String> mutate(T review);

  default String join(List<JsonPatchOperation> operations) {

    JsonPatch patch = new JsonPatch(operations);

    try {
      return mapper.writeValueAsString(patch);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("error while processing json path", e);
    }

  }
}
