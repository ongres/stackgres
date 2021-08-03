/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResource;

public abstract class AbstractLabelFactoryForBackup<T extends CustomResource<?, ?>>
    extends AbstractLabelFactory<T> implements LabelFactoryForBackup<T> {

  @Override
  public Map<String, String> backupPodLabels(T resource) {
    return ImmutableMap.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(), labelValue(resourceName(resource)),
        labelMapper().backupKey(), StackGresContext.RIGHT_VALUE);
  }

}
