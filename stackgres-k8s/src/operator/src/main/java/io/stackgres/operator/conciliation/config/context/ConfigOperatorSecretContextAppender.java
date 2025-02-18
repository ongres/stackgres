/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.config.StackGresConfigContext.Builder;
import io.stackgres.operator.conciliation.factory.config.OperatorSecret;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigOperatorSecretContextAppender
    extends ContextAppender<StackGresConfig, Builder> {

  private final ResourceFinder<Secret> secretFinder;

  public ConfigOperatorSecretContextAppender(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  public void appendContext(StackGresConfig config, Builder contextBuilder) {
    Optional<Secret> operatorSecret = secretFinder.findByNameAndNamespace(
        OperatorSecret.name(config),
        config.getMetadata().getNamespace());
    contextBuilder.operatorSecret(operatorSecret);
  }

}
