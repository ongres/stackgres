/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.factory.SubResourceFactory;

@ApplicationScoped
public class ClusterPodSecurityContext
    implements SubResourceFactory<PodSecurityContext, StackGresClusterContext> {

  public static final Long USER = 999L;
  public static final Long GROUP = 999L;

  @Override
  public PodSecurityContext createResource(StackGresClusterContext config) {
    return new PodSecurityContextBuilder()
        .withRunAsUser(USER)
        .withRunAsGroup(GROUP)
        .withRunAsNonRoot(true)
        .withFsGroup(GROUP)
        .build();
  }

}
