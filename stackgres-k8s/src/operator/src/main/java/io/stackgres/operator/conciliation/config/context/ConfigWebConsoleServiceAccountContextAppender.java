/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.config.StackGresConfigContext.Builder;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleDeployment;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigWebConsoleServiceAccountContextAppender
    extends ContextAppender<StackGresConfig, Builder> {

  private final ResourceFinder<ServiceAccount> serviceAccountFinder;

  public ConfigWebConsoleServiceAccountContextAppender(ResourceFinder<ServiceAccount> serviceAccountFinder) {
    this.serviceAccountFinder = serviceAccountFinder;
  }

  public void appendContext(StackGresConfig config, Builder contextBuilder) {
    Optional<ServiceAccount> webConsoleServiceAccount = serviceAccountFinder.findByNameAndNamespace(
        WebConsoleDeployment.name(config),
        config.getMetadata().getNamespace());
    contextBuilder.webConsoleServiceAccount(webConsoleServiceAccount);
  }

}
