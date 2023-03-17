/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class DistributedLogsPipeline
    extends AbstractMutationPipeline<StackGresDistributedLogs, StackGresDistributedLogsReview> {

  @Inject
  public DistributedLogsPipeline(
      @Any Instance<DistributedLogsMutator> mutators) {
    super(mutators);
  }

}
