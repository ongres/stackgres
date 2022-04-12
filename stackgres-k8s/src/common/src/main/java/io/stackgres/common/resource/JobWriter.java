/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.stackgres.common.CdiUtil;

@ApplicationScoped
public class JobWriter extends AbstractResourceWriter<Job, JobList, ScalableResource<Job>> {

  @Inject
  public JobWriter(KubernetesClient client) {
    super(client);
  }

  public JobWriter() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected MixedOperation<Job, JobList, ScalableResource<Job>>
      getResourceEndpoints(KubernetesClient client) {
    return client.batch().v1().jobs();
  }

}
