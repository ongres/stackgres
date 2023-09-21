/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBinding;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder
public class ServiceBindingSecret implements ResourceGenerator<StackGresClusterContext> {

  private static final String DEFAULT_SERVICE_BINDING_TYPE = "postgresql";

  private static final String DEFAULT_SERVICE_BINDING_PROVIDER = "stackgres";

  public static String name(StackGresCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName() + "-binding");
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    StackGresCluster cluster = context.getCluster();
    var binding = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBinding);

    String username = binding
        .map(StackGresClusterServiceBinding::getUsername)
        .orElse(getPgUsernameFromSuperUserCredentials(context));
    String password = context.getUserPasswordForBinding()
        .orElse(getPgUserPasswordFromSuperUserCredentials(context));
    return Stream.of(new SecretBuilder()
      .withType(getServiceBindingType())
      .withNewMetadata()
      .withName(name(cluster))
      .withNamespace(cluster.getMetadata().getNamespace())
      .endMetadata()
      .addToStringData("type", DEFAULT_SERVICE_BINDING_TYPE)
      .addToStringData("provider", binding
          .map(StackGresClusterServiceBinding::getProvider)
          .orElse(DEFAULT_SERVICE_BINDING_PROVIDER))
      .addToStringData("host", getPgHost(context))
      .addToStringData("port", getPgPort())
      .addToStringData("username", username)
      .addToStringData("password", password)
      .addToStringData("uri", buildPgConnectionUri(context,
          username,
          password,
          binding
          .map(StackGresClusterServiceBinding::getDatabase)))
      .build());
  }

  private String buildPgConnectionUri(StackGresClusterContext context, String pgUsername,
      String pgUserPassword, Optional<String> database) {
    if (database.isPresent()) {
      return String.format("postgresql://%s:%s@%s:%s/%s", pgUsername, pgUserPassword,
          getPgHost(context), getPgPort(), database.get());
    }
    return String.format("postgresql://%s:%s@%s:%s", pgUsername, pgUserPassword,
        getPgHost(context), getPgPort());
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

}
