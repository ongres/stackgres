/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutatorWeight;

@ApplicationScoped
@MutatorWeight(10)
public class DefaultDisableClusterProfileMigratorMutator implements DistributedLogsMutator {

  private static final long VERSION_1_2 = StackGresVersion.V_1_2.getVersionAsNumber();

  private JsonPointer nonProductionOptionsPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String nonProductionOptionsJson = getJsonMappingField("nonProductionOptions",
        StackGresDistributedLogsSpec.class);

    nonProductionOptionsPointer = SPEC_POINTER.append(nonProductionOptionsJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresDistributedLogsReview review) {
    Operation operation = review.getRequest().getOperation();
    final StackGresDistributedLogs distributedLogs = review.getRequest().getObject();
    final long version = StackGresVersion.getStackGresVersionAsNumber(distributedLogs);
    if (version <= VERSION_1_2 && operation == Operation.UPDATE
        && Optional.ofNullable(distributedLogs.getSpec())
        .map(StackGresDistributedLogsSpec::getNonProductionOptions)
        .map(StackGresDistributedLogsNonProduction::getDisableClusterResourceRequirements)
        .isEmpty()) {
      if (distributedLogs.getSpec().getNonProductionOptions() == null) {
        distributedLogs.getSpec().setNonProductionOptions(
            new StackGresDistributedLogsNonProduction());
      }
      distributedLogs.getSpec().getNonProductionOptions()
          .setDisablePatroniResourceRequirements(true);
      distributedLogs.getSpec().getNonProductionOptions()
          .setDisableClusterResourceRequirements(true);
      return List.of(new ReplaceOperation(nonProductionOptionsPointer,
          FACTORY.pojoNode(distributedLogs.getSpec().getNonProductionOptions())));
    }
    return List.of();
  }

}
