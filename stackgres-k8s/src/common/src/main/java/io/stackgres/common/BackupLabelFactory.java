/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgbackup.StackGresBackup;

@ApplicationScoped
public class BackupLabelFactory extends AbstractLabelFactoryForBackup<StackGresBackup> {

  private final LabelMapperForBackup<StackGresBackup> labelMapper;

  @Inject
  public BackupLabelFactory(LabelMapperForBackup<StackGresBackup> labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public LabelMapperForBackup<StackGresBackup> labelMapper() {
    return labelMapper;
  }

}
