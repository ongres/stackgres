/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutatorWeight;

@ApplicationScoped
@MutatorWeight(10)
public class DisableClusterResourceRequirementsMigratorMutator implements ClusterMutator {

  private static final long VERSION_1_2 = StackGresVersion.V_1_2.getVersionAsNumber();

  private JsonPointer nonProductionOptionsPointer;
  private JsonPointer disableClusterResourceRequirementsPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String nonProductionOptionsJson = getJsonMappingField("nonProductionOptions",
        StackGresClusterSpec.class);
    String disableClusterResourceRequirementsPointerJson =
        getJsonMappingField("disableClusterResourceRequirements",
            StackGresClusterNonProduction.class);

    nonProductionOptionsPointer = SPEC_POINTER
        .append(nonProductionOptionsJson);
    disableClusterResourceRequirementsPointer = nonProductionOptionsPointer
        .append(disableClusterResourceRequirementsPointerJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      final StackGresCluster cluster = review.getRequest().getObject();
      final long version = StackGresVersion.getStackGresVersionAsNumber(cluster);
      if (version <= VERSION_1_2) {
        if (Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getNonProductionOptions)
            .isEmpty()) {
          return List.of(
              applyAddValue(
                  nonProductionOptionsPointer,
                  FACTORY.objectNode()),
              applyAddValue(
                  disableClusterResourceRequirementsPointer,
                  FACTORY.booleanNode(true)));
        }

        if (Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getNonProductionOptions)
            .map(StackGresClusterNonProduction::getDisableClusterResourceRequirements)
            .filter(Boolean::booleanValue)
            .isEmpty()) {
          return List.of(
              applyAddValue(
                  disableClusterResourceRequirementsPointer,
                  FACTORY.booleanNode(true)));
        } else {
          return List.of(
              applyReplaceValue(
                  disableClusterResourceRequirementsPointer,
                  FACTORY.booleanNode(true)));
        }
      }
    }
    return List.of();
  }

}
