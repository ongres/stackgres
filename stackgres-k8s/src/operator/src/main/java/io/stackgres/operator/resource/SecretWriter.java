/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.DoneableSecret;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Namespaceable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;

@ApplicationScoped
public class SecretWriter extends AbstractResourceWriter<Secret, SecretList, DoneableSecret> {

  @Inject
  public SecretWriter(KubernetesClientFactory clientFactory) {
    super(clientFactory);
  }

  public SecretWriter() {
    super(null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  protected Namespaceable<NonNamespaceOperation<Secret, SecretList,
        DoneableSecret, Resource<Secret, DoneableSecret>>> getResourceEndpoints(
      KubernetesClient client) {
    return client.secrets();
  }

}
