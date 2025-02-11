/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterDatabaseSecretContextAppenderTest {

  private ClusterDatabaseSecretContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterDatabaseSecretContextAppender(secretFinder);
  }

  @Test
  void givenClusterWithoutDatabaseSecret_shouldPass() {
    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder).findByNameAndNamespace(any(), any());
    verify(contextBuilder).databaseSecret(Optional.empty());
  }

  @Test
  void givenClusterWithDatabaseSecret_shouldRetrieveItAndPass() {
    final Optional<Secret> databaseSecret = Optional.of(new SecretBuilder()
        .withData(Map.of())
        .build());
    when(secretFinder.findByNameAndNamespace(
        cluster.getMetadata().getName(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(databaseSecret);
    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder).findByNameAndNamespace(any(), any());
    verify(contextBuilder).databaseSecret(databaseSecret);
  }

}
