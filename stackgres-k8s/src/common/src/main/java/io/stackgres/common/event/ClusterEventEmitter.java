/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import java.util.Map;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterEventEmitter extends AbstractEventEmitter<StackGresCluster> {

  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  public ClusterEventEmitter(LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  protected Map<String, String> getLabels(StackGresCluster involvedObject) {
    return labelFactory.genericLabels(involvedObject);
  }

}
