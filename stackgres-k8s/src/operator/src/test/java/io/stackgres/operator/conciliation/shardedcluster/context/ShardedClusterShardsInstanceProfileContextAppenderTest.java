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

import java.util.Optional;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterShardsInstanceProfileContextAppenderTest {

  private ShardedClusterShardsInstanceProfileContextAppender contextAppender;

  private DefaultProfileFactory defaultProfileFactory = new DefaultProfileFactory();

  private StackGresShardedCluster cluster;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresProfile> profileFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    contextAppender = new ShardedClusterShardsInstanceProfileContextAppender(
        profileFinder,
        new DefaultProfileFactory());
  }

  @Test
  void givenClusterWithProfile_shouldPass() {
    final var profile = Optional.of(
        new StackGresProfileBuilder()
        .withNewSpec()
        .endSpec()
        .build());
    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(profile);
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).shardsProfile(profile);
  }

  @Test
  void givenClusterWithoutProfile_shouldFail() {
    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("SGInstanceProfile size-s was not found", ex.getMessage());
  }

  @Test
  void givenClusterWithoutDefaultProfile_shouldPass() {
    cluster.getSpec().getShards().setSgInstanceProfile(defaultProfileFactory.getDefaultResourceName(cluster));
    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).shardsProfile(Optional.empty());
  }

}
