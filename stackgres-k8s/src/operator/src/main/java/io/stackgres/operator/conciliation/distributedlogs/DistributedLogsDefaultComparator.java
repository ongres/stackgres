/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.DefaultComparator;

@ApplicationScoped
@ReconciliationScope(value = StackGresDistributedLogs.class, kind = "HasMetadata")
public class DistributedLogsDefaultComparator extends DefaultComparator {

}
