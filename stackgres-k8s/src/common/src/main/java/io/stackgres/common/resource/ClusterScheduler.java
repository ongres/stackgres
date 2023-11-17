/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterScheduler extends
    AbstractCustomResourceScheduler<StackGresCluster, StackGresClusterList> {

  public ClusterScheduler() {
    super(StackGresCluster.class, StackGresClusterList.class);
  }

  @Override
  public StackGresCluster update(StackGresCluster resource) {
    if (resource.getMetadata().getResourceVersion() == null) {
      return super.update(resource);
    }
    return client.resources(StackGresCluster.class, StackGresClusterList.class)
        .resource(resource)
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace();
  }

}
