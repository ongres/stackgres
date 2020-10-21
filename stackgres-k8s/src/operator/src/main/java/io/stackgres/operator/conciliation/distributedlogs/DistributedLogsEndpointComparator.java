/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.EndpointComparator;

@ReconciliationScope(value = StackGresDistributedLogs.class, kind = "Endpoints")
@ApplicationScoped
public class DistributedLogsEndpointComparator extends EndpointComparator {

}
