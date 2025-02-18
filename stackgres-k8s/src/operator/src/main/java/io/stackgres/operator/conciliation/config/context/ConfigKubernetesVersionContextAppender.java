/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.config.StackGresConfigContext.Builder;
import io.stackgres.operator.conciliation.factory.KubernetesVersionProvider;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigKubernetesVersionContextAppender
    extends ContextAppender<StackGresConfig, Builder> {

  private final KubernetesVersionProvider kubernetesVersionSupplier;

  public ConfigKubernetesVersionContextAppender(KubernetesVersionProvider kubernetesVersionSupplier) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
  }

  @Override
  public void appendContext(StackGresConfig cluster, Builder contextBuilder) {
    contextBuilder.kubernetesVersion(kubernetesVersionSupplier.get());
  }

}
