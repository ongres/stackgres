/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgscript.StackGresScript;

public interface LabelMapperForScript
    extends LabelMapper<StackGresScript> {

  default String streamKey(StackGresScript resource) {
    return getKeyPrefix(resource) + StackGresContext.SCRIPT_KEY;
  }

}
