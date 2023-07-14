/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class PodWriter implements ResourceWriter<Pod> {

  private final KubernetesClient client;

  @Inject
  public PodWriter(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Pod create(@NotNull Pod resource) {
    return client.pods().resource(resource).create();
  }

  @Override
  public Pod update(@NotNull Pod resource) {
    return client.pods().resource(resource).patch();
  }

  @Override
  public void delete(@NotNull Pod resource) {
    client.pods().resource(resource).delete();
  }

}
