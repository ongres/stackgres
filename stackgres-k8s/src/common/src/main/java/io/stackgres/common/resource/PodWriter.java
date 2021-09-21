/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Deletable;
import io.fabric8.kubernetes.client.dsl.PodResource;
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
    return withEndpoint(resource, endpoint -> endpoint.create(resource));
  }

  @Override
  public Pod update(@NotNull Pod resource) {
    return withEndpoint(resource, endpoint -> endpoint.patch(resource));
  }

  @Override
  public void delete(@NotNull Pod resource) {
    withEndpoint(resource, Deletable::delete);
  }

  private <T> T withEndpoint(
      Pod pod,
      Function<PodResource<Pod>, T> func) {
    String namespace = pod.getMetadata().getNamespace();
    String name = pod.getMetadata().getName();
    var endpoint = client.pods().inNamespace(namespace).withName(name);
    return func.apply(endpoint);
  }

}
