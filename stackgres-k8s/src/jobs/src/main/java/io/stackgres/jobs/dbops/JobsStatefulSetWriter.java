/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.stackgres.common.resource.AbstractResourceWriter;

@Priority(1)
@Alternative
@ApplicationScoped
public class JobsStatefulSetWriter
    extends AbstractResourceWriter<StatefulSet> {

  @Inject
  public JobsStatefulSetWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<StatefulSet, ?, ?> getResourceEndpoints(KubernetesClient client) {
    return client.apps().statefulSets();
  }

}
