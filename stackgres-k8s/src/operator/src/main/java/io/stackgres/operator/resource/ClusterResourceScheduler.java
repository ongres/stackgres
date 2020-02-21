/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDoneable;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;

@ApplicationScoped
public class ClusterResourceScheduler extends
    AbstractKubernetesCustomResourceScheduler<StackGresCluster,
      StackGresClusterList, StackGresClusterDoneable> {

  @Inject
  public ClusterResourceScheduler(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresClusterDefinition.NAME, StackGresCluster.class,
        StackGresClusterList.class, StackGresClusterDoneable.class);
  }

  public ClusterResourceScheduler() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
