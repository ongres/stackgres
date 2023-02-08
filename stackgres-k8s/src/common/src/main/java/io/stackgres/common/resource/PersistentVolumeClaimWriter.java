/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClient;
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
    return client.persistentVolumeClaims().resource(resource).create();
  }

  @Override
  public PersistentVolumeClaim update(@NotNull PersistentVolumeClaim resource) {
    return client.persistentVolumeClaims().resource(resource).patch();
  }

  @Override
  public void delete(@NotNull PersistentVolumeClaim resource) {
    client.persistentVolumeClaims().resource(resource).delete();
  }

}
