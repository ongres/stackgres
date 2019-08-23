/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresources.sgcluster;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class StackGresClusterDoneable extends CustomResourceDoneable<StackGresCluster> {

  public StackGresClusterDoneable(StackGresCluster resource,
      Function<StackGresCluster, StackGresCluster> function) {
    super(resource, function);
  }

}
