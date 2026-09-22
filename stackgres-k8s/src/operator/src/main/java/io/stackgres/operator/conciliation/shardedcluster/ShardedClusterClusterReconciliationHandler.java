/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ReconciliationScope(value = StackGresShardedCluster.class, kind = StackGresCluster.KIND)
@ApplicationScoped
public class ShardedClusterClusterReconciliationHandler
    extends AbstractReconciliationHandler<StackGresShardedCluster> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(ShardedClusterClusterReconciliationHandler.class);

  @Inject
  public ShardedClusterClusterReconciliationHandler(KubernetesClient client) {
    super(client);
  }

  @Override
  public final void delete(StackGresShardedCluster context, HasMetadata resource) {
    if (!(resource instanceof StackGresCluster cluster)) {
      throw new IllegalArgumentException(
          "Resource must be a " + StackGresCluster.KIND + " instance");
    }
    doDownscaleToZero(context, cluster);
  }

  @Override
  public final void deleteWithOrphans(StackGresShardedCluster context, HasMetadata resource) {
    if (!(resource instanceof StackGresCluster cluster)) {
      throw new IllegalArgumentException(
          "Resource must be a " + StackGresCluster.KIND + " instance");
    }
    doDownscaleToZero(context, cluster);
  }

  private void doDownscaleToZero(StackGresShardedCluster context, StackGresCluster cluster) {
    LOGGER.debug("Scaling " + StackGresCluster.KIND + " " + cluster.getMetadata().getName() +  " to 0 since deleted");
    cluster.getSpec().setInstances(0);
    patch(context, cluster, cluster);
  }

}
