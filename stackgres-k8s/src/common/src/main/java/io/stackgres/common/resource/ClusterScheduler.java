/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;

@ApplicationScoped
public class ClusterScheduler extends
    AbstractCustomResourceScheduler<StackGresCluster, StackGresClusterList> {

  public ClusterScheduler() {
    super(StackGresCluster.class, StackGresClusterList.class);
  }

  @Override
  public StackGresCluster update(StackGresCluster resource) {
    return client.resources(StackGresCluster.class, StackGresClusterList.class)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace(resource);
  }

}
