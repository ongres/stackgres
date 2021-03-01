/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;

@ApplicationScoped
public class ClusterResourceScheduler extends
    AbstractCustomResourceScheduler<StackGresCluster, StackGresClusterList> {

  @Inject
  public ClusterResourceScheduler(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresCluster.class, StackGresClusterList.class);
  }

  public ClusterResourceScheduler() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
