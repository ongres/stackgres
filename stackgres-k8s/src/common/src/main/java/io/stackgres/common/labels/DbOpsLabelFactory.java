/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;

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
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().dbOpsKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public LabelMapperForDbOps labelMapper() {
    return labelMapper;
  }

}
