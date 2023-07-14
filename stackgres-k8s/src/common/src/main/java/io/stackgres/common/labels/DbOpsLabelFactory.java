/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DbOpsLabelFactory
    extends AbstractLabelFactory<StackGresDbOps> implements LabelFactoryForDbOps {

  private final LabelMapperForDbOps labelMapper;

  @Inject
  public DbOpsLabelFactory(LabelMapperForDbOps labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public Map<String, String> dbOpsPodLabels(StackGresDbOps resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
      labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)),
      labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
      labelMapper().dbOpsKey(resource), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public LabelMapperForDbOps labelMapper() {
    return labelMapper;
  }

}
