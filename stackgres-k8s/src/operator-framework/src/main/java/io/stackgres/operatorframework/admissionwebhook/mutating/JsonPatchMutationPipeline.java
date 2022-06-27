/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import org.jetbrains.annotations.NotNull;

public interface JsonPatchMutationPipeline<T extends AdmissionReview<?>> {

  ObjectMapper JSON_MAPPER = JsonMapper.builder()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .build();

  @NotNull
  Optional<@NotNull String> mutate(@NotNull T review);

  @NotNull
  static Optional<@NotNull String> join(@NotNull List<@NotNull JsonPatchOperation> operations) {
    if (operations.isEmpty()) {
      return Optional.empty();
    }
    JsonPatch patch = new JsonPatch(operations);
    try {
      return Optional.of(JSON_MAPPER.writeValueAsString(patch));
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException("error while processing json path", e);
    }
  }

  static <T> Comparator<T> weightComparator() {
    return Comparator.comparingInt(mutator -> {
      final MutatorWeight annotation = mutator.getClass().getAnnotation(MutatorWeight.class);
      return annotation == null ? 0 : annotation.value();
    });
  }

}
