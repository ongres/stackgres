/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;

public abstract class PodSecurityFactory {

  public static final Long USER = 999L;
  public static final Long GROUP = 999L;

  public PodSecurityContext createPodSecurityContext() {
    PodSecurityContextBuilder podSecurityContextBuilder = new PodSecurityContextBuilder()
        .withRunAsNonRoot(true);
    if (!OperatorProperty.USE_ARBITRARY_USER.getBoolean()) {
      podSecurityContextBuilder
          .withRunAsUser(USER)
          .withRunAsGroup(GROUP)
          .withFsGroup(GROUP);
    }
    return podSecurityContextBuilder.build();
  }

}
