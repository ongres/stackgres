/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.ServiceAccountComparator;
import jakarta.enterprise.context.ApplicationScoped;

@ReconciliationScope(value = StackGresCluster.class, kind = "ServiceAccount")
@ApplicationScoped
public class ClusterServiceAccountComparator extends ServiceAccountComparator {

}
