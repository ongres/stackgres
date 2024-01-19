/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresConfigContext extends GenerationContext<StackGresConfig> {

  Optional<VersionInfo> getKubernetesVersion();

  Optional<Secret> getOperatorSecret();

  Optional<Secret> getWebConsoleSecret();

  Optional<Secret> getWebConsoleAdminSecret();

  Optional<ServiceAccount> getWebConsoleServiceAccount();

  boolean isGrafanaEmbedded();

  boolean isGrafanaIntegrated();

  boolean isGrafanaIntegrationJobFailed();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  Optional<String> getGrafanaUser();

  Optional<String> getGrafanaPassword();

}
