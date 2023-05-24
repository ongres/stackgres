/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;

@ApplicationScoped
public class ClusterFinder extends AbstractCustomResourceFinder<StackGresCluster> {

  @Inject
  public ClusterFinder(KubernetesClient client) {
    super(client, StackGresCluster.class, StackGresClusterList.class);
  }

}
