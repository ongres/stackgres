/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import java.util.Map;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.labels.LabelFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedDbOpsEventEmitter extends AbstractEventEmitter<StackGresShardedDbOps> {

  private final LabelFactory<StackGresShardedDbOps> labelFactory;

  @Inject
  public ShardedDbOpsEventEmitter(LabelFactory<StackGresShardedDbOps> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  protected Map<String, String> getLabels(StackGresShardedDbOps involvedObject) {
    return labelFactory.genericLabels(involvedObject);
  }

}
