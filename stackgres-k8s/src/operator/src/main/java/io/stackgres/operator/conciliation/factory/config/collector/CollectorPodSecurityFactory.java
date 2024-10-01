/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CollectorPodSecurityFactory {

  public static final Long COLLECTOR_USER = 1000L;
  public static final Long COLLECTOR_GROUP = 1000L;
  public static final Long KUBECTL_USER = 1000L;
  public static final Long KUBECTL_GROUP = 1000L;

  private final OperatorPropertyContext operatorContext;

  @Inject
  public CollectorPodSecurityFactory(OperatorPropertyContext operatorContext) {
    this.operatorContext = operatorContext;
  }

  public PodSecurityContext createCollectorPodSecurityContext(StackGresConfigContext context) {
    PodSecurityContextBuilder podSecurityContextBuilder = new PodSecurityContextBuilder()
        .withRunAsNonRoot(true);
    if (!operatorContext.getBoolean(OperatorProperty.USE_ARBITRARY_USER)) {
      podSecurityContextBuilder
          .withFsGroup(COLLECTOR_GROUP);
    }
    return podSecurityContextBuilder.build();
  }

  public SecurityContext createCollectorSecurityContext(StackGresConfigContext context) {
    SecurityContextBuilder securityContextBuilder = new SecurityContextBuilder()
        .withRunAsNonRoot(true);
    if (!operatorContext.getBoolean(OperatorProperty.USE_ARBITRARY_USER)) {
      securityContextBuilder
          .withRunAsUser(COLLECTOR_USER)
          .withRunAsGroup(COLLECTOR_GROUP);
    }
    return securityContextBuilder.build();
  }

  public SecurityContext createCollectorControllerSecurityContext(StackGresConfigContext context) {
    SecurityContextBuilder securityContextBuilder = new SecurityContextBuilder()
        .withRunAsNonRoot(true);
    if (!operatorContext.getBoolean(OperatorProperty.USE_ARBITRARY_USER)) {
      securityContextBuilder
          .withRunAsUser(KUBECTL_USER)
          .withRunAsGroup(KUBECTL_GROUP);
    }
    return securityContextBuilder.build();
  }

}
