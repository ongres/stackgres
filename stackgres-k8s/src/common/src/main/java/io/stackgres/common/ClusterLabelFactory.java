/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
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

  public static Map<String, String> patroniClusterLabels(HasMetadata resource) {
    return ImmutableMap.of(StackGresContext.CLUSTER_APP_NAME, StackGresContext.CLUSTER_NAME_KEY,
        StackGresContext.CLUSTER_UID_KEY, resource.getMetadata().getName(),
        StackGresContext.CLUSTER_KEY, StackGresContext.RIGHT_VALUE);
  }

}
