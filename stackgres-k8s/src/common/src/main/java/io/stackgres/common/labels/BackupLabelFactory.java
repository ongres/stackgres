/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.common.resource.ResourceUtil.labelValue;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackup.StackGresBackup;

@ApplicationScoped
public class BackupLabelFactory extends AbstractLabelFactory<StackGresBackup>
    implements LabelFactoryForBackup {

  private final LabelMapperForBackup labelMapper;

  @Inject
  public BackupLabelFactory(LabelMapperForBackup labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public Map<String, String> backupPodLabels(StackGresBackup resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().backupKey(resource), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public LabelMapperForBackup labelMapper() {
    return labelMapper;
  }

}
