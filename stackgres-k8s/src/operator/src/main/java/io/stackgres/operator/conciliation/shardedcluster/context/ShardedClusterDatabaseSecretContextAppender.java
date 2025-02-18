/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterDatabaseSecretContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final ResourceFinder<Secret> secretFinder;

  public ShardedClusterDatabaseSecretContextAppender(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    final Optional<Secret> databaseSecret =
        secretFinder.findByNameAndNamespace(
            cluster.getMetadata().getName(),
            cluster.getMetadata().getNamespace());
    contextBuilder
        .databaseSecret(databaseSecret);
  }

}
