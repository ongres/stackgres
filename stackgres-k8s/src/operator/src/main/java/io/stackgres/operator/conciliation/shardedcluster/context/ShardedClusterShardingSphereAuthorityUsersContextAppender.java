/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinatorConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphere;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereAuthority;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ShardedClusterShardingSphereAuthorityUsersContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final ResourceFinder<Secret> secretFinder;

  public ShardedClusterShardingSphereAuthorityUsersContextAppender(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    final List<Tuple2<String, String>> shardingSphereAuthorityUsers =
        Optional.of(cluster.getSpec().getCoordinator().getConfigurationsForCoordinator())
        .map(StackGresShardedClusterCoordinatorConfigurations::getShardingSphere)
        .map(StackGresShardedClusterShardingSphere::getAuthority)
        .map(StackGresShardedClusterShardingSphereAuthority::getUsers)
        .stream()
        .flatMap(List::stream)
        .map(user -> Tuple.tuple(
            Optional.of(secretFinder
                .findByNameAndNamespace(
                    user.getUser().getName(),
                    cluster.getMetadata().getNamespace())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Secret " + user.getUser().getName() + " not found for ShardingSphere authority user")))
            .map(secret -> secret.getData().get(user.getUser().getKey()))
            .map(ResourceUtil::decodeSecret)
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + user.getUser().getName() + " do not contains key "
                    + user.getUser().getKey() + " for ShardingSphere authority user")),
            Optional.of(secretFinder
                .findByNameAndNamespace(
                    user.getPassword().getName(),
                    cluster.getMetadata().getNamespace())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Secret " + user.getPassword().getName() + " not found for ShardingSphere authority password")))
            .map(secret -> secret.getData().get(user.getPassword().getKey()))
            .map(ResourceUtil::decodeSecret)
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + user.getPassword().getName() + " do not contains key "
                    + user.getPassword().getKey() + " for ShardingSphere authority password"))))
        .toList();

    contextBuilder
        .shardingSphereAuthorityUsers(shardingSphereAuthorityUsers);
  }

}
