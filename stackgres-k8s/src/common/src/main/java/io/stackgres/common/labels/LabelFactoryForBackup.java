/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.crd.sgbackup.StackGresBackup;

public interface LabelFactoryForBackup
    extends LabelFactory<StackGresBackup> {

  Map<String, String> backupPodLabels(StackGresBackup resource);

  @Override
  LabelMapperForBackup labelMapper();

}
