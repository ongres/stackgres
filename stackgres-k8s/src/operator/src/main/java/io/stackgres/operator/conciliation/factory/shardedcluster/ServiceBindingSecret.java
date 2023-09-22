/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBinding;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder
public class ServiceBindingSecret implements ResourceGenerator<StackGresShardedClusterContext> {

  private static final String DEFAULT_SERVICE_BINDING_TYPE = "postgresql";

  private static final String DEFAULT_SERVICE_BINDING_PROVIDER = "stackgres";

  public static String name(StackGresShardedCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName() + "-binding");
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getSource();
    var binding = Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getBinding);

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

  private String buildPgConnectionUri(StackGresShardedClusterContext context, String pgUsername,
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

  private String getPgHost(StackGresShardedClusterContext context) {
    return StackGresShardedClusterUtil
        .primaryCoordinatorServiceName(context.getSource())
        .concat(".")
        .concat(context.getSource().getMetadata().getNamespace());
  }

  private String getPgUsernameFromSuperUserCredentials(StackGresShardedClusterContext context) {
    return ShardedClusterSecret.getSuperuserCredentials(context).v1;
  }

  private String getPgUserPasswordFromSuperUserCredentials(StackGresShardedClusterContext context) {
    return ShardedClusterSecret.getSuperuserCredentials(context).v2;
  }

}
