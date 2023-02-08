/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.resource.ResourceWriter;
import org.jetbrains.annotations.NotNull;

@Priority(1)
@Alternative
@ApplicationScoped
public class OperatorPersistentVolumeClaimWriter implements ResourceWriter<PersistentVolumeClaim> {

  private final KubernetesClient client;

  @Inject
  public OperatorPersistentVolumeClaimWriter(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public PersistentVolumeClaim create(@NotNull PersistentVolumeClaim resource) {
    return client.persistentVolumeClaims()
        .resource(resource)
        .create();
  }

  @Override
  public PersistentVolumeClaim update(@NotNull PersistentVolumeClaim resource) {
    return client.persistentVolumeClaims()
        .resource(resource)
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace();
  }

  @Override
  public void delete(@NotNull PersistentVolumeClaim resource) {
    client.persistentVolumeClaims()
        .resource(resource)
        .delete();
  }
}
