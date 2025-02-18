/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupClusterInstanceProfileContextAppenderTest {

  private ShardedBackupClusterInstanceProfileContextAppender contextAppender;

  private StackGresShardedCluster cluster;

  private StackGresProfile profile;

  @Spy
  private StackGresShardedBackupContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresProfile> profileFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    profile = Fixtures.instanceProfile().loadSizeS().get();
    contextAppender = new ShardedBackupClusterInstanceProfileContextAppender(
        profileFinder);
  }

  @Test
  void givenClusterWithInstanceProfile_shouldPass() {
    when(profileFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getSgInstanceProfile(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(profile));
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).foundProfile(Optional.of(profile));
  }

  @Test
  void givenClusterWithoutInstanceProfile_shouldFail() {
    when(profileFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getSgInstanceProfile(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("SGInstanceProfile size-s was not found", ex.getMessage());
  }

}
