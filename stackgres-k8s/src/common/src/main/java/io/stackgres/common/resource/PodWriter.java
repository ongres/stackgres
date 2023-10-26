/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;

@ApplicationScoped
public class PodWriter
    extends AbstractResourceWriter<Pod> {

  @Inject
  public PodWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<Pod, ?, ?> getResourceEndpoints(KubernetesClient client) {
    return client.pods();
  }

}
