/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface Converter {

  ObjectNode convert(long originalVersion, long desiredVersion, ObjectNode node);

}
