/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsPipeline
    extends AbstractMutationPipeline<StackGresDistributedLogs, StackGresDistributedLogsReview> {

  @Inject
  public DistributedLogsPipeline(
      @Any Instance<DistributedLogsMutator> mutators) {
    super(mutators);
  }

}
