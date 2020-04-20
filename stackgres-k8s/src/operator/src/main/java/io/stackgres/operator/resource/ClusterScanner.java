/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDefinition;
import io.stackgres.common.crd.sgcluster.StackGresClusterDoneable;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;

@ApplicationScoped
public class ClusterScanner
    extends AbstractCustomResourceScanner<StackGresCluster, StackGresClusterList,
    StackGresClusterDoneable> {

  /**
   * Create a {@code ClusterScanner} instance.
   */
  @Inject
  public ClusterScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresClusterDefinition.NAME,
        StackGresCluster.class, StackGresClusterList.class,
        StackGresClusterDoneable.class);
  }

  public ClusterScanner() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
