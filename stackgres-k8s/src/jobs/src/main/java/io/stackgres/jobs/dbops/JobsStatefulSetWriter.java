/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.resource.ResourceWriter;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@Priority(1)
@Alternative
@ApplicationScoped
public class JobsStatefulSetWriter implements ResourceWriter<StatefulSet> {

  private final KubernetesClient client;

  @Inject
  public JobsStatefulSetWriter(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public StatefulSet create(@NotNull StatefulSet resource) {
    return client.apps().statefulSets()
        .resource(resource)
        .create();
  }

  @Override
  public StatefulSet update(@NotNull StatefulSet resource) {
    return client.apps().statefulSets()
        .resource(resource)
        .patch();
  }

  @Override
  public void delete(@NotNull StatefulSet resource) {
    client.apps().statefulSets()
        .resource(resource)
        .delete();
  }

}
