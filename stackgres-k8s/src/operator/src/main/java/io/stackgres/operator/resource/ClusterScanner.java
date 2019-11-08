/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDoneable;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;

import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple5;

@ApplicationScoped
public class ClusterScanner
    extends AbstractKubernetesCustomResourceScanner<StackGresCluster, StackGresClusterList> {

  private final KubernetesClient client;

  /**
   * Create a {@code ClusterScanner} instance.
   */
  @Inject
  public ClusterScanner(KubernetesClient client) {
    this.client = client;
  }

  @Override
  protected Tuple5<KubernetesClient, String, Class<StackGresCluster>,
      Class<StackGresClusterList>, Class<? extends Doneable<StackGresCluster>>> arguments() {
    return Tuple.tuple(client, StackGresClusterDefinition.NAME,
        StackGresCluster.class, StackGresClusterList.class,
        StackGresClusterDoneable.class);
  }

}
