/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.configuration.OperatorPropertyContext;

@ApplicationScoped
public class ClusterPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresClusterContext, PodSecurityContext> {

  @Inject
  public ClusterPodSecurityFactory(OperatorPropertyContext operatorContext) {
    super(operatorContext);
  }

  @Override
  public PodSecurityContext createResource(StackGresClusterContext source) {
    return createPodSecurityContext();
  }

}
