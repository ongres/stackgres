/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgconfig.StackGresConfig;

public interface LabelMapperForConfig
    extends LabelMapper<StackGresConfig> {

  default String backupKey(StackGresConfig resource) {
    return getKeyPrefix(resource) + StackGresContext.CONFIG_KEY;
  }

}
