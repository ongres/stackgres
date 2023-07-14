/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.ServiceAccountComparator;
import jakarta.enterprise.context.ApplicationScoped;

@ReconciliationScope(value = StackGresDistributedLogs.class, kind = "ServiceAccount")
@ApplicationScoped
public class DistributedLogsServiceAccountComparator extends ServiceAccountComparator {
}
