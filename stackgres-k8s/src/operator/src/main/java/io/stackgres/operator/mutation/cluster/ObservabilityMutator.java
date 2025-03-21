/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterObservability;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ObservabilityMutator implements ClusterMutator {

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
    if (resource.getSpec().getConfigurations().getObservability() == null) {
      resource.getSpec().getConfigurations().setObservability(new StackGresClusterObservability());
    }

    var oldObservability = Optional.ofNullable(review.getRequest().getOldObject())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getObservability);
    if (oldObservability
        .map(StackGresClusterObservability::getDisableMetrics)
        .map(disableMetrics -> disableMetrics.equals(
            resource.getSpec().getConfigurations().getObservability().getDisableMetrics()))
        .orElse(resource.getSpec().getConfigurations().getObservability().getDisableMetrics() == null)) {
      resource.getSpec().getConfigurations().getObservability()
          .setDisableMetrics(resource.getSpec().getPods().getDisableMetricsExporter());
    }
    resource.getSpec().getPods().setDisableMetricsExporter(
        resource.getSpec().getConfigurations().getObservability().getDisableMetrics());

    return resource;
  }

}
