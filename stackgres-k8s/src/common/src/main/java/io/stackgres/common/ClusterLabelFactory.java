/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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

}
