/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public interface JsonUtil {

  /**
   * Deep merge a JSON value with another JSON value removing from the latter
   *  any leaf value (an array is considered leaf) based on the provided model class.
   */
  static ObjectNode mergeJsonObjectsFilteringByModel(
      ObjectNode value,
      ObjectNode otherValue,
      Class<?> modelClass,
      ObjectMapper objectMapper) {
    try {
      ObjectNode otherValueFilteredByModel = objectMapper.valueToTree(
          objectMapper.readValue(otherValue.toString(), modelClass));
      List<Tuple2<ObjectNode, ObjectNode>> nodesToFilterOut =
          List.of(Tuple.tuple(otherValueFilteredByModel, otherValue));
      while (!nodesToFilterOut.isEmpty()) {
        nodesToFilterOut
            .forEach(nodeTuple -> Seq.seq(nodeTuple.v1.fieldNames())
               .filter(Predicate.not(field -> nodeTuple.v2.get(field) instanceof ObjectNode))
               .forEach(nodeTuple.v2::remove));
        nodesToFilterOut = Seq.seq(nodesToFilterOut)
            .flatMap(nodeTuple -> Seq.seq(nodeTuple.v1.fieldNames())
                .map(field -> Tuple.tuple(nodeTuple.v1.get(field), nodeTuple.v2.get(field))))
            .filter(nodeTuple -> ObjectNode.class.isInstance(nodeTuple.v1))
            .map(nodeTuple -> nodeTuple.map1(ObjectNode.class::cast).map2(ObjectNode.class::cast))
            .toList();
      }
      JsonNode updatedValue = objectMapper.readerForUpdating(otherValue)
          .readTree(objectMapper.writeValueAsString(value));
      return (ObjectNode) updatedValue;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
