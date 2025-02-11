/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBindingBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterServiceBindingUserPasswordContextAppenderTest {

  private ClusterServiceBindingUserPasswordContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterServiceBindingUserPasswordContextAppender(secretFinder);
  }

  @Test
  void givenClusterWithoutServiceBindingUserSecret_shouldPass() {
    contextAppender.appendContext(cluster, contextBuilder);

    verify(contextBuilder).userPasswordForBinding(Optional.empty());
  }

  @Test
  void givenClusterWithServiceBindingPasswordSecret_shouldRetrieveItAndPass() {
    cluster.getSpec().getConfigurations().setBinding(
        new StackGresClusterServiceBindingBuilder()
        .withNewPassword()
        .withName("test")
        .withKey("user-password")
        .endPassword()
        .build());
    final Optional<Secret> serviceBdindingPasswordSecret = Optional.of(new SecretBuilder()
        .withData(ResourceUtil.encodeSecret(Map.of("user-password", "test")))
        .build());
    when(secretFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(serviceBdindingPasswordSecret);
    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder).findByNameAndNamespace(any(), any());
    verify(contextBuilder).userPasswordForBinding(Optional.of("test"));
  }

  @Test
  void givenClusterWithMissingServiceBindingPasswordSecret_shouldFail() {
    cluster.getSpec().getConfigurations().setBinding(
        new StackGresClusterServiceBindingBuilder()
        .withNewPassword()
        .withName("test")
        .withKey("user-password")
        .endPassword()
        .build());
    when(secretFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Service Binding password secret test was not found", ex.getMessage());
  }

  @Test
  void givenClusterWithMissingServiceBindingPasswordSecretKey_shouldFail() {
    cluster.getSpec().getConfigurations().setBinding(
        new StackGresClusterServiceBindingBuilder()
        .withNewPassword()
        .withName("test")
        .withKey("user-password")
        .endPassword()
        .build());
    final Optional<Secret> serviceBdindingPasswordSecret = Optional.of(new SecretBuilder()
        .withData(ResourceUtil.encodeSecret(Map.of()))
        .build());
    when(secretFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(serviceBdindingPasswordSecret);
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Service Binding password key user-password was not found in secret test", ex.getMessage());
  }

}
