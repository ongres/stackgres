/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels.v14;

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

}
