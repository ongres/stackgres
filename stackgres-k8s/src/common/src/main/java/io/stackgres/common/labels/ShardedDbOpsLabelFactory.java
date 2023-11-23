/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedDbOpsLabelFactory
    extends AbstractLabelFactory<StackGresShardedDbOps> implements LabelFactoryForShardedDbOps {

  private final LabelMapperForShardedDbOps labelMapper;

  @Inject
  public ShardedDbOpsLabelFactory(LabelMapperForShardedDbOps labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public Map<String, String> shardedDbOpsPodLabels(StackGresShardedDbOps resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().dbOpsKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> shardedDbOpsLabels(StackGresShardedDbOps resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().dbOpsKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public LabelMapperForShardedDbOps labelMapper() {
    return labelMapper;
  }

}
