/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterScanner
    extends AbstractCustomResourceScanner<StackGresCluster, StackGresClusterList> {

  /**
   * Create a {@code ClusterScanner} instance.
   */
  @Inject
  public ClusterScanner(KubernetesClient client) {
    super(client, StackGresCluster.class, StackGresClusterList.class);
  }

}
