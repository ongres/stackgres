/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public interface LabelMapperForDbOps extends LabelMapper {

  default String dbOpsKey() {
    return StackGresContext.DB_OPS_KEY;
  }

}
