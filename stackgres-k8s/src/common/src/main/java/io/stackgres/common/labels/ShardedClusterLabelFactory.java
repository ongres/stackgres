/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ShardedClusterLabelFactory
    extends AbstractLabelFactory<StackGresShardedCluster>
    implements LabelFactoryForShardedCluster {

  private final LabelMapperForShardedCluster labelMapper;

  @Inject
  public ShardedClusterLabelFactory(LabelMapperForShardedCluster labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public Map<String, String> coordinatorLabels(@NotNull StackGresShardedCluster resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
      labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)),
      labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
      labelMapper().coordinatorKey(resource), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> shardsLabels(@NotNull StackGresShardedCluster resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
      labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)),
      labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
      labelMapper().shardsKey(resource), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public LabelMapperForShardedCluster labelMapper() {
    return labelMapper;
  }

}
