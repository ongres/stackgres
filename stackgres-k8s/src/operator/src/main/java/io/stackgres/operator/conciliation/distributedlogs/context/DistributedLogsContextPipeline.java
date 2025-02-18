/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs.context;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.ContextPipeline;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class DistributedLogsContextPipeline
    extends ContextPipeline<StackGresDistributedLogs, Builder> {

  public DistributedLogsContextPipeline(Instance<ContextAppender<StackGresDistributedLogs, Builder>> contextAppenders) {
    super(contextAppenders);
  }

}
