/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import static io.stackgres.operator.conversion.ConversionUtil.apiVersionAsNumber;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public interface ConversionPipeline {

  default List<ObjectNode> convert(String desiredVersion, List<ObjectNode> object) {
    long desiredVersionAsNumber = apiVersionAsNumber(desiredVersion);
    Stream<Tuple2<Long, ObjectNode>> objectStream = object.stream()
        .map(node -> Tuple.tuple(apiVersionAsNumber(node.get("apiVersion").asText()), node));
    for (Converter converter : getConverters()) {
      objectStream = objectStream
          .map(t -> t.map2(node -> converter.convert(t.v1, desiredVersionAsNumber, node)));
    }
    return objectStream
        .map(Tuple2::v2)
        .map(node -> node.put("apiVersion", desiredVersion))
        .collect(Collectors.toUnmodifiableList());
  }

  List<Converter> getConverters();

}
