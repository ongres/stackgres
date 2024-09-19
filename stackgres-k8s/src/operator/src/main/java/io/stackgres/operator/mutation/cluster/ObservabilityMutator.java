/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterObservability;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ObservabilityMutator implements ClusterMutator {

  private static final long V_1_13 = StackGresVersion.V_1_13.getVersionAsNumber();

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    long version = StackGresVersion.getStackGresVersionFromResourceAsNumber(resource);
    if (version <= V_1_13) {
      if (resource.getSpec() == null) {
        resource.setSpec(new StackGresClusterSpec());
      }
      if (resource.getSpec().getConfigurations() == null) {
        resource.getSpec().setConfigurations(new StackGresClusterConfigurations());
      }
      if (resource.getSpec().getConfigurations().getObservability() == null) {
        resource.getSpec().getConfigurations().setObservability(new StackGresClusterObservability());
      }
      resource.getSpec().getConfigurations().getObservability()
          .setDiableMetrics(resource.getSpec().getPods().getDisableMetricsExporter());
      resource.getSpec().getPods().setDisableMetricsExporter(null);
      resource.getSpec().getConfigurations().getObservability()
          .setPrometheusAutobind(resource.getSpec().getPrometheusAutobind());
      resource.getSpec().setPrometheusAutobind(null);
    }
    return resource;
  }

}
