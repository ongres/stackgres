/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;

@ApplicationScoped
public class ClusterScheduler extends
    AbstractCustomResourceScheduler<StackGresCluster, StackGresClusterList> {

  @Inject
  public ClusterScheduler(KubernetesClient client) {
    super(client, StackGresCluster.class, StackGresClusterList.class);
  }

  public ClusterScheduler() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  public StackGresCluster update(StackGresCluster resource) {
    return client.customResources(StackGresCluster.class, StackGresClusterList.class)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace(resource);
  }

}
