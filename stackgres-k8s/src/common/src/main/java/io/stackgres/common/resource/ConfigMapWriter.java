/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfigMapWriter extends AbstractResourceWriter<ConfigMap> {

  @Inject
  public ConfigMapWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<ConfigMap, ?, ?> getResourceEndpoints(KubernetesClient client) {
    return client.configMaps();
  }

}
