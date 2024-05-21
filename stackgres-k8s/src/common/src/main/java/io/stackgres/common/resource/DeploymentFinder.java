/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DeploymentFinder extends AbstractResourceFinderAndScanner<Deployment> {

  @Inject
  public DeploymentFinder(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<
          Deployment,
          ? extends KubernetesResourceList<Deployment>,
          ? extends Resource<Deployment>> getOperation(
      KubernetesClient client) {
    return client.apps().deployments();
  }

}
