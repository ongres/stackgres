/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.cluster.ClusterConciliator;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterConciliatorTest extends ConciliatorTest<StackGresCluster> {

  private static final StackGresCluster cluster = JsonUtil
      .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);

  @Mock
  private RequiredResourceGenerator<StackGresCluster> requiredResourceGenerator;

  @Mock
  private DeployedResourcesScanner<StackGresCluster> deployedResourcesScanner;

  @Override
  protected Conciliator<StackGresCluster> buildConciliator(List<HasMetadata> required,
                                                           List<HasMetadata> deployed) {


    when(requiredResourceGenerator.getRequiredResources(cluster))
        .thenReturn(required);
    when(deployedResourcesScanner.getDeployedResources(cluster))
        .thenReturn(deployed);

    final ClusterConciliator clusterConciliator = new ClusterConciliator();
    clusterConciliator.setRequiredResourceGenerator(requiredResourceGenerator);
    clusterConciliator.setDeployedResourcesScanner(deployedResourcesScanner);
    clusterConciliator.setResourceComparator(resourceComparator);
    return clusterConciliator;
  }

  @Override
  protected StackGresCluster getConciliationResource() {
    return cluster;
  }
}