/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import java.util.List;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import io.stackgres.operatorframework.admissionwebhook.Operation;

public class DefaultPostgresMutator
    extends AbstractDefaultResourceMutator<StackGresPostgresConfig,
        StackGresDistributedLogs, StackGresDistributedLogsReview>
    implements DistributedLogsConfigurationMutator {

  public DefaultPostgresMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig> resourceFactory,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresDistributedLogsReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      operations.addAll(ensureConfigurationNode(review));
      operations.addAll(super.mutate(review));
      return operations.build();

    }
    return List.of();
  }

  @Override
  protected String getTargetPropertyValue(StackGresDistributedLogs targetDistributedLogs) {
    return targetDistributedLogs.getSpec().getConfiguration().getPostgresConfig();
  }

  @Override
  public JsonPointer getTargetPointer() {
    return getConfigurationTargetPointer("postgresConfig");
  }
}
