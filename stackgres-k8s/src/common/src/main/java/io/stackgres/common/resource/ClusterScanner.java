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
public class ClusterScanner
    extends AbstractCustomResourceScanner<StackGresCluster, StackGresClusterList> {

  /**
   * Create a {@code ClusterScanner} instance.
   */
  @Inject
  public ClusterScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresCluster.class, StackGresClusterList.class);
  }

  public ClusterScanner() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
