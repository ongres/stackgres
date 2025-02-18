/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.factory.KubernetesVersionProvider;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterKubernetesVersionContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final KubernetesVersionProvider kubernetesVersionSupplier;

  public ShardedClusterKubernetesVersionContextAppender(KubernetesVersionProvider kubernetesVersionSupplier) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    contextBuilder.kubernetesVersion(kubernetesVersionSupplier.get());
  }

}
