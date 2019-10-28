/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.ResourceCreator;

@ApplicationScoped
public class DefaultCreator implements ResourceCreator {

  @Override
  public void createOrReplace(KubernetesClient client, HasMetadata resource) {
    client.resource(resource).createOrReplace();
  }
}
