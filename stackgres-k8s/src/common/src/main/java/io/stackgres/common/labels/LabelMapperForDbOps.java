/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;

public interface LabelMapperForDbOps
    extends LabelMapper<StackGresDbOps> {

  default String dbOpsKey(StackGresDbOps resource) {
    return getKeyPrefix(resource) + StackGresContext.DB_OPS_KEY;
  }

}
