/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

public interface LabelFactoryForDistributedLogs
    extends LabelFactory<StackGresDistributedLogs> {

  Map<String, String> defaultConfigLabels(StackGresDistributedLogs resource);

  @Override
  LabelMapperForDistributedLogs labelMapper();

}
