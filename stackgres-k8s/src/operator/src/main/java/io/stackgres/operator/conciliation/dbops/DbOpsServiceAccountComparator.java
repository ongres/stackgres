/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.ServiceAccountComparator;
import jakarta.enterprise.context.ApplicationScoped;

@ReconciliationScope(value = StackGresDbOps.class, kind = "ServiceAccount")
@ApplicationScoped
public class DbOpsServiceAccountComparator extends ServiceAccountComparator {

}
