/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels.v14;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.ClusterLabelMapper;
import org.jetbrains.annotations.NotNull;

public class ClusterLabelFactoryV14 extends AbstractLabelFactoryForCluster<StackGresCluster> {

  private final ClusterLabelMapper labelMapper;

  public ClusterLabelFactoryV14(ClusterLabelMapper labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public ClusterLabelMapper labelMapper() {
    return labelMapper;
  }

  @Override
  public String resourceScope(@NotNull StackGresCluster resource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, String> patroniClusterLabelsWithoutScope(StackGresCluster resource) {
    return patroniClusterLabels(resource);
  }

  @Override
  public Map<String, String> patroniPrimaryLabelsWithoutScope(StackGresCluster resource) {
    return patroniPrimaryLabels(resource);
  }

  @Override
  public Map<String, String> patroniReplicaLabelsWithoutScope(StackGresCluster resource) {
    return patroniReplicaLabels(resource);
  }

  @Override
  public Map<String, String> patroniReplicaLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(super.patroniReplicaLabels(resource))
        .put(PatroniUtil.NOLOADBALANCE_TAG, PatroniUtil.FALSE_TAG_VALUE)
        .build();
  }

}
