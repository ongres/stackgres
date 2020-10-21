/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.CronJobComparator;

@ReconciliationScope(value = StackGresCluster.class, kind = "CronJob")
@ApplicationScoped
public class ClusterCronJobComparator extends CronJobComparator {

}
