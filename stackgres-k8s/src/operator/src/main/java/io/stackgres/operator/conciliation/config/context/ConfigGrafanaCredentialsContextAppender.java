/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigGrafana;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.config.StackGresConfigContext.Builder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigGrafanaCredentialsContextAppender
    extends ContextAppender<StackGresConfig, Builder> {

  private final ResourceFinder<Secret> secretFinder;

  public ConfigGrafanaCredentialsContextAppender(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  public void appendContext(StackGresConfig config, Builder contextBuilder) {
    Optional<Map<String, String>> grafanaCredentials = Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getGrafana)
        .filter(grafana -> grafana.getSecretNamespace() != null
            && grafana.getSecretName() != null
            && grafana.getSecretUserKey() != null
            && grafana.getSecretPasswordKey() != null)
        .map(grafana -> secretFinder.findByNameAndNamespace(
            grafana.getSecretName(), grafana.getSecretNamespace())
            .orElseThrow(() -> new IllegalArgumentException(
                "Can not find secret "
                    + grafana.getSecretNamespace() + "." + grafana.getSecretName()
                    + " for grafana credentials")))
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret);
    Optional<String> grafanaUser = grafanaCredentials
        .map(credentials -> credentials.get(
            config.getSpec().getGrafana().getSecretUserKey()))
        .or(() -> Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getGrafana)
            .map(StackGresConfigGrafana::getUser));
    Optional<String> grafanaPassword = grafanaCredentials
        .map(credentials -> credentials.get(
            config.getSpec().getGrafana().getSecretPasswordKey()))
        .or(() -> Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getGrafana)
            .map(StackGresConfigGrafana::getPassword));
    contextBuilder
        .grafanaUser(grafanaUser)
        .grafanaPassword(grafanaPassword);
  }

}
