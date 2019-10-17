/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KindLiteral;
import io.stackgres.common.ResourceCreator;

@ApplicationScoped
public class ResourceCreationSelectorImpl implements ResourceCreationSelector {

  private Instance<ResourceCreator> creators;

  @Inject
  public ResourceCreationSelectorImpl(@Any Instance<ResourceCreator> creators) {
    this.creators = creators;
  }

  @Override
  public void createOrReplace(KubernetesClient client, HasMetadata resource) {

    Instance<ResourceCreator> customCreator = creators
        .select(new KindLiteral(resource.getKind()));

    if (customCreator.isResolvable()) {
      customCreator.get().createOrReplace(client, resource);
    } else {
      creators.select(DefaultCreator.class).get().createOrReplace(client, resource);
    }

  }
}
