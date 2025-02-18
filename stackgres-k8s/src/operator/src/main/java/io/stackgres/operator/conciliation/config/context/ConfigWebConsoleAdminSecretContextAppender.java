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
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleAdminSecret;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigWebConsoleAdminSecretContextAppender
    extends ContextAppender<StackGresConfig, Builder> {

  private final ResourceFinder<Secret> secretFinder;

  public ConfigWebConsoleAdminSecretContextAppender(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  public void appendContext(StackGresConfig config, Builder contextBuilder) {
    Optional<Secret> webConsoleAdminSecret = secretFinder.findByNameAndNamespace(
        WebConsoleAdminSecret.sourceName(config),
        config.getMetadata().getNamespace());
    contextBuilder.webConsoleAdminSecret(webConsoleAdminSecret);
  }

}
