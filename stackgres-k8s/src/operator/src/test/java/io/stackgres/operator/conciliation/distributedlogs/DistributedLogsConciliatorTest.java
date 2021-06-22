/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.ConciliatorTest;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsConciliatorTest extends ConciliatorTest<StackGresDistributedLogs> {

  private static final StackGresDistributedLogs distributedLogs = JsonUtil
      .readFromJson("distributedlogs/default.json", StackGresDistributedLogs.class);

  @Mock
  private RequiredResourceGenerator<StackGresDistributedLogs> requiredResourceGenerator;

  @Mock
  private DeployedResourcesScanner<StackGresDistributedLogs> deployedResourcesScanner;


  @Override
  protected Conciliator<StackGresDistributedLogs> buildConciliator(List<HasMetadata> required,
                                                                   List<HasMetadata> deployed) {

    when(requiredResourceGenerator.getRequiredResources(distributedLogs))
        .thenReturn(required);
    when(deployedResourcesScanner.getDeployedResources(distributedLogs))
        .thenReturn(deployed);

    final DistributedLogsConciliator clusterConciliator = new DistributedLogsConciliator();
    clusterConciliator.setRequiredResourceGenerator(requiredResourceGenerator);
    clusterConciliator.setDeployedResourcesScanner(deployedResourcesScanner);
    clusterConciliator.setResourceComparator(resourceComparator);
    return clusterConciliator;
  }

  @Override
  protected StackGresDistributedLogs getConciliationResource() {
    return distributedLogs;
  }
}