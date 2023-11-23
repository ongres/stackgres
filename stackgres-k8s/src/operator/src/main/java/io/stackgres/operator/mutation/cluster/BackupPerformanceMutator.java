/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupPerformanceMutator implements ClusterMutator {

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    Optional.of(resource.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBackups)
        .stream()
        .flatMap(List::stream)
        .map(StackGresClusterBackupConfiguration::getPerformance)
        .filter(performance -> performance != null)
        .forEach(performance -> {
          if (performance.getMaxDiskBandwitdh() != null) {
            if (performance.getMaxDiskBandwidth() == null) {
              performance.setMaxDiskBandwidth(performance.getMaxDiskBandwitdh());
            }
            performance.setMaxDiskBandwitdh(null);
          }
          if (performance.getMaxNetworkBandwitdh() != null) {
            if (performance.getMaxNetworkBandwidth() == null) {
              performance.setMaxNetworkBandwidth(performance.getMaxNetworkBandwitdh());
            }
            performance.setMaxNetworkBandwitdh(null);
          }
        });
    return resource;
  }

}
