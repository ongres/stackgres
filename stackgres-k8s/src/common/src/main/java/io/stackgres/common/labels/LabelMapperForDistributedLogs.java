/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

public interface LabelMapperForDistributedLogs
    extends LabelMapper<StackGresDistributedLogs> {

  default String defaultConfigKey(StackGresDistributedLogs resource) {
    return getKeyPrefix(resource) + StackGresContext.DISTRIBUTED_LOGS_DEFAULT_CONFIG_KEY;
  }

}
