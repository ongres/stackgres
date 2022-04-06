/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.ComparisonDelegator;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public class ClusterPatchResumer extends PatchResumer<StackGresCluster> {

  public ClusterPatchResumer(ComparisonDelegator<StackGresCluster> resourceComparator) {
    super(resourceComparator);
  }

  @Override
  protected Tuple2<HasMetadata, String> resumeResourcePatch(HasMetadata resource,
      JsonNode patch, StackGresCluster cluster) {
    if (resource.getKind().equals(HasMetadata.getKind(StatefulSet.class))
        && patch.get("op").asText().equals("replace")
        && patch.get("path").asText().equals("/spec/replicas")) {
      int replicas = ((StatefulSet) resource).getSpec().getReplicas();
      if (cluster.getSpec().getInstances() != replicas) {
        return Tuple.tuple(resource, "Instances have been updated to "
            + ((StatefulSet) resource).getSpec().getReplicas()
            + " (cluster instances are now " + cluster.getSpec().getInstances() + ")");
      } else {
        return Tuple.tuple(resource, "Instances have been updated to "
          + replicas);
      }
    }
    return Tuple.tuple(resource, resumeFieldPatch(patch,
        resource.getKind().equals(HasMetadata.getKind(Secret.class))));
  }

}
