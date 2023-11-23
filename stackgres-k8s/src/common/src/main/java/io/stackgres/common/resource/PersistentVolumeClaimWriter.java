/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PersistentVolumeClaimWriter
    extends AbstractResourceWriter<PersistentVolumeClaim> {

  @Inject
  public PersistentVolumeClaimWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<PersistentVolumeClaim, ?, ?> getResourceEndpoints(
      KubernetesClient client) {
    return client.persistentVolumeClaims();
  }

}
