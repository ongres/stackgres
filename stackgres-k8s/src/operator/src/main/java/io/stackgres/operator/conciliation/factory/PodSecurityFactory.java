/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operator.configuration.OperatorPropertyContext;

public abstract class PodSecurityFactory {

  public static final Long USER = 999L;
  public static final Long GROUP = 999L;

  private OperatorPropertyContext operatorContext;

  public PodSecurityContext createPodSecurityContext() {
    PodSecurityContextBuilder podSecurityContextBuilder = new PodSecurityContextBuilder()
        .withRunAsNonRoot(true);
    if (!operatorContext.getBoolean(OperatorProperty.USE_ARBITRARY_USER)) {
      podSecurityContextBuilder
          .withRunAsUser(USER)
          .withRunAsGroup(GROUP)
          .withFsGroup(GROUP);
    }
    return podSecurityContextBuilder.build();
  }

  @Inject
  public void setOperatorContext(OperatorPropertyContext operatorContext) {
    this.operatorContext = operatorContext;
  }
}
