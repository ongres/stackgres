/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
  public Map<String, String> coordinatorLabels(@Nonnull StackGresShardedCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(coordinatorLabelsWithoutUid(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .build();
  }

  @Override
  public Map<String, String> coordinatorLabelsWithoutUid(@Nonnull StackGresShardedCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().coordinatorKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> shardsLabels(@Nonnull StackGresShardedCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().shardsKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> scheduledBackupPodLabels(StackGresShardedCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().scheduledShardedBackupKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public LabelMapperForShardedCluster labelMapper() {
    return labelMapper;
  }

}
