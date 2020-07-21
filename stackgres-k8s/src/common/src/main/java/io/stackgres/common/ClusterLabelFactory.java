/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgcluster.StackGresCluster;

@ApplicationScoped
public class ClusterLabelFactory extends AbstractLabelFactory<StackGresCluster> {

  private final LabelMapper<StackGresCluster> labelMapper;

  @Inject
  public ClusterLabelFactory(LabelMapper<StackGresCluster> labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public LabelMapper<StackGresCluster> getLabelMapper() {
    return labelMapper;
  }

}
