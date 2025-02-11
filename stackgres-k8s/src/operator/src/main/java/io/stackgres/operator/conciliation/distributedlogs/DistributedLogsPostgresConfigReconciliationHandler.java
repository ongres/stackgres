/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.FireAndForgetReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;

@ReconciliationScope(value = StackGresDistributedLogs.class, kind = StackGresPostgresConfig.KIND)
@ApplicationScoped
public class DistributedLogsPostgresConfigReconciliationHandler
    extends FireAndForgetReconciliationHandler<StackGresDistributedLogs> {

  private final LabelFactoryForDistributedLogs labelFactory;

  public DistributedLogsPostgresConfigReconciliationHandler(
      @ReconciliationScope(value = StackGresDistributedLogs.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresDistributedLogs> handler,
      LabelFactoryForDistributedLogs labelFactory) {
    super(handler);
    this.labelFactory = labelFactory;
  }

  @Override
  protected boolean canForget(StackGresDistributedLogs context, HasMetadata resource) {
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
