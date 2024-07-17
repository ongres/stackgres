/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamConciliator extends AbstractConciliator<StackGresStream> {

  @Inject
  public StreamConciliator(
      KubernetesClient client,
      RequiredResourceGenerator<StackGresStream> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresStream> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache) {
    super(client, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
  }

  @Override
  protected boolean skipDeletion(HasMetadata requiredResource, StackGresStream config) {
    return requiredResource instanceof Job;
  }

}
