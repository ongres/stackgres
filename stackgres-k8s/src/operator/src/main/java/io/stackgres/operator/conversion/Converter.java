/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface Converter {

  ObjectNode convert(long originalVersion, long desiredVersion, ObjectNode node);

  static void removeFieldIfExists(ObjectNode target, String fieldName) {
    Optional.ofNullable(target.get(fieldName))
        .ifPresent(o -> target.remove(fieldName));
  }

}
