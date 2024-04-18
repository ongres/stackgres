/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JobFinder extends AbstractResourceFinderAndScanner<Job> {

  @Inject
  public JobFinder(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<Job, ? extends KubernetesResourceList<Job>, ? extends Resource<Job>>
      getOperation(KubernetesClient client) {
    return client.batch().v1().jobs();
  }

}
