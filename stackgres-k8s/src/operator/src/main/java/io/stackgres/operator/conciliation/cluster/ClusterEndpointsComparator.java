/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.PatroniEndpointsComparator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresCluster.class, kind = "Endpoints")
@ApplicationScoped
public class ClusterEndpointsComparator extends PatroniEndpointsComparator {

  @Inject
  protected ClusterEndpointsComparator(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  public ClusterEndpointsComparator() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

}
