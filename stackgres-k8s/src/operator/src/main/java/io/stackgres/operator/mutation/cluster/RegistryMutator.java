/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterRegistry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Default {@code spec.configurations.registry.enabled} to {@code true} for new SGClusters and to
 * {@code false} for SGClusters created before the field existed (that keep using the images
 * bundled with the operator) so that the value is always explicit and can be validated as
 * immutable.
 */
@ApplicationScoped
public class RegistryMutator implements ClusterMutator {

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    if (resource.getSpec() == null) {
      resource.setSpec(new StackGresClusterSpec());
    }
    if (resource.getSpec().getConfigurations() == null) {
      resource.getSpec().setConfigurations(new StackGresClusterConfigurations());
    }
    if (resource.getSpec().getConfigurations().getRegistry() == null) {
      resource.getSpec().getConfigurations().setRegistry(new StackGresClusterRegistry());
    }
    if (resource.getSpec().getConfigurations().getRegistry().getEnabled() == null) {
      resource.getSpec().getConfigurations().getRegistry().setEnabled(
          review.getRequest().getOperation() == Operation.CREATE);
    }
    return resource;
  }

}
