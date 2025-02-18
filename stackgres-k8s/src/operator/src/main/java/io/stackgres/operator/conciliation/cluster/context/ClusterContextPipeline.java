/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.ContextPipeline;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class ClusterContextPipeline
    extends ContextPipeline<StackGresCluster, Builder> {

  public ClusterContextPipeline(Instance<ContextAppender<StackGresCluster, Builder>> contextAppenders) {
    super(contextAppenders);
  }

}
