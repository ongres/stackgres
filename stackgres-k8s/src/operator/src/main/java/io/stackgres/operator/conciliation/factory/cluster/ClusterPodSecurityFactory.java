/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresClusterContext, PodSecurityContext> {

  @Override
  public PodSecurityContext createResource(StackGresClusterContext source) {
    return createPodSecurityContext();
  }

}
