/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphere;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereAuthorityBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterShardingSphereAuthorityUsersContextAppenderTest {

  private ShardedClusterShardingSphereAuthorityUsersContextAppender contextAppender;

  private StackGresShardedCluster cluster;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    contextAppender = new ShardedClusterShardingSphereAuthorityUsersContextAppender(secretFinder);
  }

  @Test
  void givenClusterWithoutShardingSphereUsers_shouldPass() {
    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder, Mockito.never()).findByNameAndNamespace(any(), any());
    verify(contextBuilder).shardingSphereAuthorityUsers(List.of());
  }

  @Test
  void givenClusterWithShardingSphereUsersAndSecret_shouldRetrieveItAndPass() {
    var shardingSphere = new StackGresShardedClusterShardingSphere();
    var authority = new StackGresShardedClusterShardingSphereAuthorityBuilder()
        .addNewUser()
        .withNewUser("username", "username-secret")
        .withNewPassword("password", "password-secret")
        .endUser()
        .build();
    shardingSphere.setAuthority(authority);
    cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().setShardingSphere(shardingSphere);
    final Optional<Secret> secret = Optional.of(new SecretBuilder()
        .withData(ResourceUtil.encodeSecret(Map.of(
            "username", "test",
            "password", "1234")))
        .build());
    when(secretFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getShardingSphere().getAuthority().getUsers().get(0).getUser().getName(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(secret);
    when(secretFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getShardingSphere().getAuthority().getUsers().get(0).getPassword().getName(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(secret);
    contextAppender.appendContext(cluster, contextBuilder);

    verify(contextBuilder).shardingSphereAuthorityUsers(List.of(Tuple.tuple("test", "1234")));
  }

  @Test
  void givenClusterWithShardingSphereUsersAndMissingUserSecret_shouldFail() {
    var shardingSphere = new StackGresShardedClusterShardingSphere();
    var authority = new StackGresShardedClusterShardingSphereAuthorityBuilder()
        .addNewUser()
        .withNewUser("username", "username-secret")
        .withNewPassword("password", "password-secret")
        .endUser()
        .build();
    shardingSphere.setAuthority(authority);
    cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().setShardingSphere(shardingSphere);
    when(secretFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getShardingSphere().getAuthority().getUsers().get(0).getUser().getName(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Secret username-secret not found for ShardingSphere authority user", ex.getMessage());
  }

  @Test
  void givenClusterWithShardingSphereUsersAndMissingUserKey_shouldFail() {
    var shardingSphere = new StackGresShardedClusterShardingSphere();
    var authority = new StackGresShardedClusterShardingSphereAuthorityBuilder()
        .addNewUser()
        .withNewUser("username", "username-secret")
        .withNewPassword("password", "password-secret")
        .endUser()
        .build();
    shardingSphere.setAuthority(authority);
    cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().setShardingSphere(shardingSphere);
    final Optional<Secret> secret = Optional.of(new SecretBuilder()
        .withData(ResourceUtil.encodeSecret(Map.of(
            "password", "1234")))
        .build());
    when(secretFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getShardingSphere().getAuthority().getUsers().get(0).getUser().getName(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(secret);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Secret username-secret do not contains key username for ShardingSphere authority user",
        ex.getMessage());
  }

  @Test
  void givenClusterWithShardingSphereUsersAndMissingPasswordSecret_shouldFail() {
    var shardingSphere = new StackGresShardedClusterShardingSphere();
    var authority = new StackGresShardedClusterShardingSphereAuthorityBuilder()
        .addNewUser()
        .withNewUser("username", "username-secret")
        .withNewPassword("password", "password-secret")
        .endUser()
        .build();
    shardingSphere.setAuthority(authority);
    cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().setShardingSphere(shardingSphere);
    final Optional<Secret> secret = Optional.of(new SecretBuilder()
        .withData(ResourceUtil.encodeSecret(Map.of(
            "username", "test",
            "password", "1234")))
        .build());
    when(secretFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getShardingSphere().getAuthority().getUsers().get(0).getUser().getName(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(secret);
    when(secretFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getShardingSphere().getAuthority().getUsers().get(0).getPassword().getName(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Secret password-secret not found for ShardingSphere authority password", ex.getMessage());
  }

  @Test
  void givenClusterWithShardingSphereUsersAndMissingPasswordKey_shouldFail() {
    var shardingSphere = new StackGresShardedClusterShardingSphere();
    var authority = new StackGresShardedClusterShardingSphereAuthorityBuilder()
        .addNewUser()
        .withNewUser("username", "username-secret")
        .withNewPassword("password", "password-secret")
        .endUser()
        .build();
    shardingSphere.setAuthority(authority);
    cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().setShardingSphere(shardingSphere);
    final Optional<Secret> secret = Optional.of(new SecretBuilder()
        .withData(ResourceUtil.encodeSecret(Map.of(
            "username", "test")))
        .build());
    when(secretFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getShardingSphere().getAuthority().getUsers().get(0).getUser().getName(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(secret);
    when(secretFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getShardingSphere().getAuthority().getUsers().get(0).getPassword().getName(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(secret);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Secret password-secret do not contains key password for ShardingSphere authority password",
        ex.getMessage());
  }

}
