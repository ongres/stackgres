/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import java.util.Map;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterEventEmitter extends AbstractEventEmitter<StackGresShardedCluster> {

  private final LabelFactory<StackGresShardedCluster> labelFactory;

  @Inject
  public ShardedClusterEventEmitter(LabelFactory<StackGresShardedCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  protected Map<String, String> getLabels(StackGresShardedCluster involvedObject) {
    return labelFactory.genericLabels(involvedObject);
  }

}
