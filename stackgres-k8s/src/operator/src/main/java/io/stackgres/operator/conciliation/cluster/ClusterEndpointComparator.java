/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.EndpointComparator;
import io.stackgres.operator.conciliation.comparator.StackGresAbstractComparator;

@ReconciliationScope(value = StackGresCluster.class, kind = "Endpoints")
@ApplicationScoped
public class ClusterEndpointComparator extends EndpointComparator {

}
