/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

@ApplicationScoped
public class SecretWriter extends AbstractResourceWriter<Secret, SecretList, Resource<Secret>> {

  @Inject
  public SecretWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<Secret, SecretList, Resource<Secret>>
      getResourceEndpoints(KubernetesClient client) {
    return client.secrets();
  }

}
