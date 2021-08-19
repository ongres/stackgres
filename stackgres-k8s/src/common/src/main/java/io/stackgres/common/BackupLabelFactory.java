/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BackupLabelFactory extends AbstractLabelFactoryForBackup {

  private final LabelMapperForBackup labelMapper;

  @Inject
  public BackupLabelFactory(LabelMapperForBackup labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public LabelMapperForBackup labelMapper() {
    return labelMapper;
  }

}
