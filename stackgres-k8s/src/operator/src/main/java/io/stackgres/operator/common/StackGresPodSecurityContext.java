/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operatorframework.resource.factory.SubResourceFactory;

@ApplicationScoped
public class StackGresPodSecurityContext
    implements SubResourceFactory<PodSecurityContext, StackGresClusterContext> {

  public static final Long USER = 999L;
  public static final Long GROUP = 999L;

  @Override
  public PodSecurityContext createResource(StackGresClusterContext config) {
    PodSecurityContextBuilder podSecurityContextBuilder = new PodSecurityContextBuilder()
        .withRunAsNonRoot(true);
    if (!config.getOperatorContext().getBoolean(OperatorProperty.USE_ARBITRARY_USER)) {
      podSecurityContextBuilder
          .withRunAsUser(USER)
          .withRunAsGroup(GROUP)
          .withFsGroup(GROUP);
    }
    return podSecurityContextBuilder.build();
  }

}
