/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public class JsonUtil {

  private static final JsonMapper JSON_MAPPER = createJsonMapper();

  private static JsonMapper createJsonMapper() {
    return JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .build();
  }

  private JsonUtil() {
  }

  /**
   * Deep merge a JSON value with another JSON value removing from the latter
   *  any leaf value (an array is considered leaf) based on the provided model class.
   */
  public static ObjectNode mergeJsonObjectsFilteringByModel(
      ObjectNode value,
      ObjectNode otherValue,
      Class<?> modelClass) {
    try {
      ObjectNode otherValueFilteredByModel = JSON_MAPPER.valueToTree(
          JSON_MAPPER.readValue(otherValue.toString(), modelClass));
      List<Tuple2<ObjectNode, ObjectNode>> nodesToFilterOut =
          new ArrayList<>(List.of(Tuple.tuple(otherValueFilteredByModel, otherValue)));
      List<Tuple2<ObjectNode, ObjectNode>> nextNodesToFilterOut = nodesToFilterOut;
      while (true) {
        nextNodesToFilterOut =
            Seq.seq(nextNodesToFilterOut)
            .flatMap(nodeTuple -> Seq.seq(nodeTuple.v1.fieldNames())
                .map(field -> Tuple.tuple(nodeTuple.v1.get(field), nodeTuple.v2.get(field))))
            .filter(nodeTuple -> ObjectNode.class.isInstance(nodeTuple.v1) && nodeTuple.v2 != null)
            .map(nodeTuple -> nodeTuple.map1(ObjectNode.class::cast).map2(ObjectNode.class::cast))
            .toList();
        if (nextNodesToFilterOut.isEmpty()) {
          break;
        }
        nodesToFilterOut.addAll(0, nextNodesToFilterOut);
      }
      nodesToFilterOut
          .forEach(nodeTuple -> Seq.seq(nodeTuple.v1.fieldNames())
             .filter(Predicate.not(field -> nodeTuple.v2.get(field) instanceof ObjectNode objectNode
                 && !objectNode.isEmpty()))
             .forEach(nodeTuple.v2::remove));
      JsonNode updatedValue = JSON_MAPPER.readerForUpdating(otherValue)
          .readTree(JSON_MAPPER.writeValueAsString(value));
      return (ObjectNode) updatedValue;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
