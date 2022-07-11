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
import com.github.fge.jsonpatch.ReplaceOperation;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutatorWeight;

@ApplicationScoped
@MutatorWeight(10)
public class DefaultDisableClusterProfileMigratorMutator implements ClusterMutator {

  private static final long VERSION_1_2 = StackGresVersion.V_1_2.getVersionAsNumber();

  private JsonPointer nonProductionOptionsPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String nonProductionOptionsJson = getJsonMappingField("nonProductionOptions",
        StackGresClusterSpec.class);

    nonProductionOptionsPointer = SPEC_POINTER.append(nonProductionOptionsJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    Operation operation = review.getRequest().getOperation();
    final StackGresCluster cluster = review.getRequest().getObject();
    final long version = StackGresVersion.getStackGresVersionAsNumber(cluster);
    if (version <= VERSION_1_2 && operation == Operation.UPDATE
        && Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getNonProductionOptions)
        .map(StackGresClusterNonProduction::getDisableClusterResourceRequirements)
        .isEmpty()) {
      if (cluster.getSpec().getNonProductionOptions() == null) {
        cluster.getSpec().setNonProductionOptions(new StackGresClusterNonProduction());
      }
      cluster.getSpec().getNonProductionOptions().setDisableClusterResourceRequirements(true);
      return List.of(new ReplaceOperation(nonProductionOptionsPointer,
          FACTORY.pojoNode(cluster.getSpec().getNonProductionOptions())));
    }
    return List.of();
  }

}
