/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurationServiceBinding;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatusServiceBinding;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;

@Singleton
@OperatorVersionBinder
public class ServiceBindingSecret implements ResourceGenerator<StackGresClusterContext> {

  private static final String DEFAULT_SERVICE_BINDING_TYPE = "postgresql";

  private static final String DEFAULT_SERVICE_BINDING_PROVIDER = "stackgres";

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    StackGresCluster cluster = context.getCluster();
    var serviceBindingConfiguration = Optional.ofNullable(cluster.getSpec()
        .getConfiguration().getBinding());

    if (serviceBindingConfiguration.isPresent()) {
      return Stream.of(this.createSecretServiceBindingFromSgClusterSpecValues(context,
        serviceBindingConfiguration.get()));
    } else {
      return Stream.of(this.createSecretServiceBindingWithDefaultValues(context));
    }
  }

  private Secret createSecretServiceBindingWithDefaultValues(StackGresClusterContext context) {
    return new SecretBuilder()
      .withType(this.getServiceBindingType())
      .withNewMetadata()
      .withName(this.getServiceBindingName(context.getCluster()))
      .withNamespace(context.getCluster().getMetadata().getNamespace())
      .endMetadata()
      .addToStringData("type", DEFAULT_SERVICE_BINDING_TYPE)
      .addToStringData("provider", DEFAULT_SERVICE_BINDING_PROVIDER)
      .addToStringData("host", this.getPgHost(context))
      .addToStringData("port", this.getPgPort())
      .addToStringData("username", this.getPgUsernameFromSuperUserCredentials(context))
      .addToStringData("password", this.getPgUserPasswordFromSuperUserCredentials(context))
      .addToStringData("uri", this.buildPgConnectionUri(context,
        this.getPgUsernameFromSuperUserCredentials(context),
        this.getPgUserPasswordFromSuperUserCredentials(context), null))
      .build();
  }

  private Secret createSecretServiceBindingFromSgClusterSpecValues(StackGresClusterContext context,
      StackGresClusterConfigurationServiceBinding serviceBindingConfiguration) {
    StackGresCluster cluster = context.getCluster();

    return new SecretBuilder()
      .withType(this.getServiceBindingType())
      .withNewMetadata()
      .withName(this.getServiceBindingName(cluster))
      .withNamespace(cluster.getMetadata().getNamespace())
      .endMetadata()
      .addToStringData("type", DEFAULT_SERVICE_BINDING_TYPE)
      .addToStringData("provider", serviceBindingConfiguration.getProvider())
      .addToStringData("host", this.getPgHost(context))
      .addToStringData("port", this.getPgPort())
      .addToStringData("username", Optional.of(serviceBindingConfiguration
          .getUsername()).orElse(this.getPgUsernameFromSuperUserCredentials(context)))
      .addToStringData("password", context.getUserPasswordForBinding()
          .orElse(this.getPgUserPasswordFromSuperUserCredentials(context)))
      .addToStringData("uri", this.buildPgConnectionUri(context,
        serviceBindingConfiguration.getUsername(),
        context.getUserPasswordForBinding()
          .orElse(this.getPgUserPasswordFromSuperUserCredentials(context)),
        serviceBindingConfiguration.getDatabase()))
      .build();
  }

  private String buildPgConnectionUri(StackGresClusterContext context, String pgUsername,
      String pgUserPassword, String database) {
    if (database == null || database.isEmpty()) {
      return String.format("postgresql://%s:%s@%s:%s", pgUsername, pgUserPassword,
        this.getPgHost(context), this.getPgPort());
    }
    return String.format("postgresql://%s:%s@%s:%s/%s", pgUsername, pgUserPassword,
      this.getPgHost(context), this.getPgPort(), database);
  }

  private String getServiceBindingType() {
    return String.format("servicebinding.io/%s", DEFAULT_SERVICE_BINDING_TYPE);
  }

  private String getPgPort() {
    return String.valueOf(EnvoyUtil.PG_PORT);
  }

  private String getPgHost(StackGresClusterContext context) {
    return PatroniUtil.readWriteName(context.getCluster()).concat(".")
      .concat(context.getCluster().getMetadata().getNamespace());
  }

  private String getPgUsernameFromSuperUserCredentials(StackGresClusterContext context) {
    return PatroniSecret.getSuperuserCredentials(context).v1;
  }

  private String getPgUserPasswordFromSuperUserCredentials(StackGresClusterContext context) {
    return PatroniSecret.getSuperuserCredentials(context).v2;
  }

  private String getServiceBindingName(StackGresCluster cluster) {
    Optional<String> serviceBindingNameFromStatusSpec = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getBinding)
        .map(StackGresClusterStatusServiceBinding::getName);
    return serviceBindingNameFromStatusSpec
      .orElseGet(() -> cluster.getMetadata().getName() + "-binding");
  }
}
