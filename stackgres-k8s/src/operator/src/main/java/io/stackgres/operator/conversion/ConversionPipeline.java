/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ConversionPipeline {

  default List<ObjectNode> convert(String desiredVersion, List<ObjectNode> object) {
    return object.stream().map(node -> node.put("apiVersion", desiredVersion))
        .collect(Collectors.toUnmodifiableList());
  }
}
