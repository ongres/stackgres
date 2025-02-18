/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBinding;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterServiceBindingUserPasswordContextAppender
    extends ShardedClusterContextAppenderWithSecrets {

  public ShardedClusterServiceBindingUserPasswordContextAppender(
      ResourceFinder<Secret> secretFinder) {
    super(secretFinder);
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    final var serviceBindingConfig = Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getBinding);
    final var userPasswordForBinding = getSecretAndKeyOrThrow(
        cluster.getMetadata().getNamespace(),
        serviceBindingConfig,
        StackGresClusterServiceBinding::getPassword,
        secretKeySelector -> "Service Binding password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Service Binding password secret " + secretKeySelector.getName()
        + " was not found");
    contextBuilder
        .userPasswordForBinding(userPasswordForBinding);
  }

}
