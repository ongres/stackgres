/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.FireAndForgetReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;

@ReconciliationScope(value = StackGresCluster.class, kind = StackGresPoolingConfig.KIND)
@ApplicationScoped
public class ClusterPoolingConfigReconciliationHandler
    extends FireAndForgetReconciliationHandler<StackGresCluster> {

  private final LabelFactoryForCluster labelFactory;

  public ClusterPoolingConfigReconciliationHandler(
      @ReconciliationScope(value = StackGresCluster.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresCluster> handler,
      LabelFactoryForCluster labelFactory) {
    super(handler);
    this.labelFactory = labelFactory;
  }

  @Override
  protected boolean canForget(StackGresCluster context, HasMetadata resource) {
    return labelFactory.defaultConfigLabels(context)
        .entrySet()
        .stream()
        .allMatch(label -> Optional.of(resource.getMetadata().getLabels())
            .stream()
            .map(Map::entrySet)
            .flatMap(Set::stream)
            .anyMatch(label::equals));
  }

}
