/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import io.stackgres.operator.conciliation.factory.KubernetesVersionProvider;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterKubernetesVersionContextAppender
    extends ContextAppender<StackGresCluster, StackGresClusterContext.Builder> {

  private final KubernetesVersionProvider kubernetesVersionSupplier;

  public ClusterKubernetesVersionContextAppender(KubernetesVersionProvider kubernetesVersionSupplier) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    contextBuilder.kubernetesVersion(kubernetesVersionSupplier.get());
  }

}
