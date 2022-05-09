/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.resource.ResourceUtil.labelValue;

import java.util.Map;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;

public abstract class AbstractLabelFactoryForDbOps
    extends AbstractLabelFactory<StackGresDbOps> implements LabelFactoryForDbOps {

  @Override
  public Map<String, String> dbOpsPodLabels(StackGresDbOps resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().dbOpsKey(resource), StackGresContext.RIGHT_VALUE);
  }

}
