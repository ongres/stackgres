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
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

public class DefaultPostgresMutator
    extends AbstractDefaultResourceMutator<StackGresPostgresConfig>
    implements DistributedLogsConfigurationMutator {

  @Override
  public List<JsonPatchOperation> mutate(StackGresDistributedLogsReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      operations.addAll(ensureConfigurationNode(review));
      operations.addAll(addDefaultResource(review));
      return operations.build();

    }
    return List.of();
  }

  @Override
  protected String getTargetPropertyValue(StackGresDistributedLogs targetDistributedLogs) {
    return targetDistributedLogs.getSpec().getConfiguration().getPostgresConfig();
  }

  @Override
  public JsonPointer getTargetPointer() throws NoSuchFieldException {
    return getTargetPointer("postgresConfig");
  }

  @Override
  public JsonPointer getTargetPointer(String field) throws NoSuchFieldException {
    return getConfigurationTargetPointer(field);
  }
}
