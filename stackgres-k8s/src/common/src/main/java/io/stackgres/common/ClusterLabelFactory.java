/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

@ApplicationScoped
public class ClusterLabelFactory extends AbstractLabelFactoryForCluster<StackGresCluster> {

  private final LabelMapperForCluster<StackGresCluster> labelMapper;

  @Inject
  public ClusterLabelFactory(LabelMapperForCluster<StackGresCluster> labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public LabelMapperForCluster<StackGresCluster> labelMapper() {
    return labelMapper;
  }

  @Override
  public Map<String, String> patroniReplicaLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(super.patroniReplicaLabels(resource))
        .put(PatroniUtil.NOLOADBALANCE_TAG, PatroniUtil.FALSE_TAG_VALUE)
        .build();
  }

}
