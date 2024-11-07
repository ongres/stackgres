/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsConciliator extends AbstractConciliator<StackGresDistributedLogs> {

  @Inject
  public DistributedLogsConciliator(
      KubernetesClient client,
      CustomResourceFinder<StackGresDistributedLogs> finder,
      RequiredResourceGenerator<StackGresDistributedLogs> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresDistributedLogs> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache,
      LabelFactoryForDistributedLogs labelFactory) {
    super(client, finder, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
  }

}
