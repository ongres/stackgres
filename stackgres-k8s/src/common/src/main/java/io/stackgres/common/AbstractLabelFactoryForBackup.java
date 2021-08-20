/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgbackup.StackGresBackup;

public abstract class AbstractLabelFactoryForBackup
    extends AbstractLabelFactory<StackGresBackup> implements LabelFactoryForBackup {

  @Override
  public Map<String, String> backupPodLabels(StackGresBackup resource) {
    return ImmutableMap.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(), labelValue(resourceName(resource)),
        labelMapper().backupKey(), StackGresContext.RIGHT_VALUE);
  }

}
