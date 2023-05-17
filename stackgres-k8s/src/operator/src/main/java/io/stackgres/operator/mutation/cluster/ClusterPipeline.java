/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class ClusterPipeline
    extends AbstractMutationPipeline<StackGresCluster, StackGresClusterReview> {

  @Inject
  public ClusterPipeline(
      @Any Instance<ClusterMutator> mutators) {
    super(mutators);
  }

}
