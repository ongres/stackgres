/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardeddbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedDbOpsMajorVersionUpgradeMutatorTest {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  @Mock
  private CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  private ShardedDbOpsReview review;
  private StackGresShardedCluster cluster;
  private ShardedDbOpsMajorVersionUpgradeMutator mutator;
  private Instant defaultTimestamp;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.shardedDbOps().loadMajorVersionUpgradeCreate().get();
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getMetadata().setNamespace(
        review.getRequest().getObject().getMetadata().getNamespace());

    defaultTimestamp = Instant.now();
    mutator = new ShardedDbOpsMajorVersionUpgradeMutator(clusterFinder, defaultTimestamp);
  }

  @Test
  void majorVersionUpgradeWithBackupPath_shouldSetNothing() {
    StackGresShardedDbOps actualDbOps = mutate(review);

    assertEquals(review.getRequest().getObject(), actualDbOps);
  }

  @Test
  void majorVersionUpgradeWithoutBackupPaths_shouldSetNothing() {
    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setBackupPaths(null);
    final StackGresShardedDbOps actualDbOps = mutate(review);

    assertEquals(review.getRequest().getObject(), actualDbOps);
  }

  @Test
  void majorVersionUpgradeWithBackupsButWithoutBackupPaths_shouldSetIt() {
    cluster.getSpec().setConfigurations(
        new StackGresShardedClusterConfigurations());
    cluster.getSpec().getConfigurations().setBackups(new ArrayList<>());
    cluster.getSpec().getConfigurations().getBackups()
        .add(new StackGresShardedClusterBackupConfiguration());
    cluster.getSpec().getConfigurations().getBackups()
        .get(0).setSgObjectStorage("test");
    cluster.getSpec().getConfigurations().getBackups()
        .get(0).setPaths(List.of("test", "test0", "test1", "test2"));
    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setBackupPaths(null);
    final StackGresShardedDbOps actualDbOps = mutate(review);

    final StackGresShardedDbOps dbOps = review.getRequest().getObject();
    final String postgresVersion = dbOps.getSpec()
        .getMajorVersionUpgrade().getPostgresVersion();
    final String postgresFlavor = cluster.getSpec()
        .getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .getMajorVersion(postgresVersion);
    assertEquals(
        Seq.range(0, cluster.getSpec().getShards().getClusters() + 1)
            .map(index -> BackupStorageUtil.getPath(
                cluster.getMetadata().getNamespace(),
                StackGresShardedClusterUtil.getClusterName(cluster, index),
                defaultTimestamp,
                postgresMajorVersion))
        .toList(),
        actualDbOps.getSpec().getMajorVersionUpgrade().getBackupPaths());
  }

  @Test
  void majorVersionUpgradeWithBackupsButWithSomeBackupPaths_shouldSetIt() {
    cluster.getSpec().setConfigurations(
        new StackGresShardedClusterConfigurations());
    cluster.getSpec().getConfigurations().setBackups(new ArrayList<>());
    cluster.getSpec().getConfigurations().getBackups()
        .add(new StackGresShardedClusterBackupConfiguration());
    cluster.getSpec().getConfigurations().getBackups()
        .get(0).setSgObjectStorage("test");
    cluster.getSpec().getConfigurations().getBackups()
        .get(0).setPaths(List.of("test", "test0", "test1", "test2"));
    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setBackupPaths(
        List.of("test", "test0"));
    final StackGresShardedDbOps actualDbOps = mutate(review);

    final StackGresShardedDbOps dbOps = review.getRequest().getObject();
    final String postgresVersion = dbOps.getSpec()
        .getMajorVersionUpgrade().getPostgresVersion();
    final String postgresFlavor = cluster.getSpec()
        .getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .getMajorVersion(postgresVersion);
    assertEquals(
        Seq.of("test", "test0")
        .append(Seq.range(2, cluster.getSpec().getShards().getClusters() + 1)
            .map(index -> BackupStorageUtil.getPath(
                cluster.getMetadata().getNamespace(),
                StackGresShardedClusterUtil.getClusterName(cluster, index),
                defaultTimestamp,
                postgresMajorVersion)))
        .toList(),
        actualDbOps.getSpec().getMajorVersionUpgrade().getBackupPaths());
  }

  @Test
  void majorVersionUpgradeWithBackupsAndWithBackupPaths_shouldDoNothing() {
    cluster.getSpec().setConfigurations(
        new StackGresShardedClusterConfigurations());
    cluster.getSpec().getConfigurations().setBackups(new ArrayList<>());
    cluster.getSpec().getConfigurations().getBackups()
        .add(new StackGresShardedClusterBackupConfiguration());
    cluster.getSpec().getConfigurations().getBackups()
        .get(0).setSgObjectStorage("test");
    cluster.getSpec().getConfigurations().getBackups()
        .get(0).setPaths(List.of("test", "test0", "test1", "test2"));
    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setBackupPaths(
        List.of("test", "test0", "test1", "test2"));
    final StackGresShardedDbOps actualDbOps = mutate(review);

    assertEquals(
        List.of("test", "test0", "test1", "test2"),
        actualDbOps.getSpec().getMajorVersionUpgrade().getBackupPaths());
  }

  private StackGresShardedDbOps mutate(ShardedDbOpsReview review) {
    return mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));
  }
}
