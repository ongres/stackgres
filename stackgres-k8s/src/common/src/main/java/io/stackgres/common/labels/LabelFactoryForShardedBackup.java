/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;

public interface LabelFactoryForShardedBackup
    extends LabelFactory<StackGresShardedBackup> {

  Map<String, String> backupPodLabels(StackGresShardedBackup resource);

  @Override
  LabelMapperForShardedBackup labelMapper();

}
