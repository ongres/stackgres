/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResource;

public abstract class AbstractLabelFactoryForDbOps<T extends CustomResource<?, ?>>
    extends AbstractLabelFactory<T> implements LabelFactoryForDbOps<T> {

  @Override
  public Map<String, String> dbOpsPodLabels(T resource) {
    return ImmutableMap.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(), labelValue(resourceName(resource)),
        labelMapper().dbOpsKey(), StackGresContext.RIGHT_VALUE);
  }

}
