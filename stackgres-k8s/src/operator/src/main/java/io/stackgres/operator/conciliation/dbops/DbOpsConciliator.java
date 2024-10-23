/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DbOpsConciliator extends AbstractConciliator<StackGresDbOps> {

  @Inject
  public DbOpsConciliator(
      KubernetesClient client,
      CustomResourceFinder<StackGresDbOps> finder,
      RequiredResourceGenerator<StackGresDbOps> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresDbOps> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache) {
    super(client, finder, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
  }

  @Override
  protected boolean skipDeletion(HasMetadata requiredResource, StackGresDbOps config) {
    return requiredResource instanceof Job;
  }

}
