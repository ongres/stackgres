/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.ConfigContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.common.PrometheusContext;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresConfigContext extends GenerationContext<StackGresConfig>, ConfigContext {

  @Override
  default StackGresConfig getConfig() {
    return getSource();
  }

  Optional<VersionInfo> getKubernetesVersion();

  Optional<Secret> getOperatorSecret();

  Optional<Secret> getWebConsoleSecret();

  Optional<Secret> getWebConsoleAdminSecret();

  Optional<ServiceAccount> getWebConsoleServiceAccount();

  boolean isGrafanaEmbedded();

  boolean isGrafanaIntegrated();

  boolean isGrafanaIntegrationJobFailed();

  Optional<Secret> getCollectorSecret();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  Optional<String> getGrafanaUser();

  Optional<String> getGrafanaPassword();

  List<ObservedClusterContext> getObservedClusters();

  List<PrometheusContext> getPrometheus();

  public static class Builder extends ImmutableStackGresConfigContext.Builder {
  }

  public static Builder builder() {
    return new Builder();
  }

}
