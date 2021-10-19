/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Deletable;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class PersistentVolumeClaimWriter implements ResourceWriter<PersistentVolumeClaim> {

  private final KubernetesClient client;

  @Inject
  public PersistentVolumeClaimWriter(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public PersistentVolumeClaim create(@NotNull PersistentVolumeClaim resource) {
    return withEndpoint(resource, endpoint -> endpoint.create(resource));
  }

  @Override
  public PersistentVolumeClaim update(@NotNull PersistentVolumeClaim resource) {
    return withEndpoint(resource, endpoint -> endpoint.patch(resource));
  }

  @Override
  public void delete(@NotNull PersistentVolumeClaim resource) {
    withEndpoint(resource, Deletable::delete);
  }

  private <T> T withEndpoint(
      PersistentVolumeClaim pod,
      Function<Resource<PersistentVolumeClaim>, T> func) {
    String namespace = pod.getMetadata().getNamespace();
    String name = pod.getMetadata().getName();
    var endpoint = client.persistentVolumeClaims().inNamespace(namespace).withName(name);
    return func.apply(endpoint);
  }

}
