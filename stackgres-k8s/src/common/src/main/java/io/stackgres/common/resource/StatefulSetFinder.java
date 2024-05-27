/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatefulSetFinder extends AbstractResourceFinderAndScanner<StatefulSet> {

  @Inject
  public StatefulSetFinder(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<StatefulSet, ? extends KubernetesResourceList<StatefulSet>, ? extends Resource<StatefulSet>>
      getOperation(KubernetesClient client) {
    return client.apps().statefulSets();
  }

}
