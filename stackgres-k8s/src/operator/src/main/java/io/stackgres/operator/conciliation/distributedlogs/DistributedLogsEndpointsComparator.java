/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.EndpointsComparator;

@ReconciliationScope(value = StackGresDistributedLogs.class, kind = "Endpoints")
@ApplicationScoped
public class DistributedLogsEndpointsComparator extends EndpointsComparator {

  @Inject
  public DistributedLogsEndpointsComparator(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  public DistributedLogsEndpointsComparator() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

}
