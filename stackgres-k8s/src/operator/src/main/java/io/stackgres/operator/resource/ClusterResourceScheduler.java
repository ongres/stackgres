/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDoneable;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;

@ApplicationScoped
public class ClusterResourceScheduler extends
    AbstractCustomResourceScheduler<StackGresCluster,
        StackGresClusterList, StackGresClusterDoneable> {

  public ClusterResourceScheduler() {
    super(StackGresClusterDefinition.NAME, StackGresCluster.class,
        StackGresClusterList.class, StackGresClusterDoneable.class);
  }

}
