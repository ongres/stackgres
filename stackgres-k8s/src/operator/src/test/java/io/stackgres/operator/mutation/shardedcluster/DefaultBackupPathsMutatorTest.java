/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.operator.common.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultBackupPathsMutatorTest {

  private static final String POSTGRES_VERSION = "14.4";

  private StackGresShardedClusterReview review;
  private DefaultBackupPathsMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().getObject().getSpec()
        .setConfigurations(new StackGresShardedClusterConfigurations());
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    mutator = new DefaultBackupPathsMutator();
  }

  @Test
  void clusterWithBackupPath_shouldSetNothing() {
    StackGresShardedCluster actualCluster = mutate(review);
    assertEquals(review.getRequest().getObject(), actualCluster);
  }

  @Test
  void clusterWithoutBackupPath_shouldSetIt() {
    final StackGresShardedCluster cluster = review.getRequest().getObject();
    cluster.getMetadata().setAnnotations(
        Map.of(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion()));
    var backupConfiguration = new StackGresShardedClusterBackupConfiguration();
    backupConfiguration.setSgObjectStorage("backupconf");
    cluster.getSpec().getConfigurations().setBackups(List.of(backupConfiguration));

    final StackGresShardedCluster actualCluster = mutate(review);

    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .getMajorVersion(postgresVersion);
    assertEquals(
        Seq.range(0, cluster.getSpec().getShards().getClusters() + 1)
        .map(index -> BackupStorageUtil.getPath(
            cluster.getMetadata().getNamespace(),
            StackGresShardedClusterForCitusUtil.getClusterName(cluster, index),
            postgresMajorVersion))
        .toList(),
        actualCluster.getSpec().getConfigurations().getBackups().get(0).getPaths());
  }

  @Test
  void clusterWithBackupsPath_shouldSetNothing() {
    review.getRequest().getObject().getSpec().getConfigurations().setBackups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getConfigurations().getBackups()
        .add(new StackGresShardedClusterBackupConfiguration());
    review.getRequest().getObject().getSpec().getConfigurations().getBackups()
        .get(0).setPaths(List.of("test-0", "test-1", "test-2"));
    StackGresShardedCluster actualCluster = mutate(review);

    assertEquals(review.getRequest().getObject(), actualCluster);
  }

  @Test
  void clusterWithPartialBackupsPath_shouldSetNewOnes() {
    final StackGresShardedCluster cluster = review.getRequest().getObject();
    review.getRequest().getObject().getSpec().getConfigurations().setBackups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getConfigurations().getBackups()
        .add(new StackGresShardedClusterBackupConfiguration());
    review.getRequest().getObject().getSpec().getConfigurations().getBackups()
        .get(0).setPaths(List.of("test-0", "test-1"));
    StackGresShardedCluster actualCluster = mutate(review);

    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .getMajorVersion(postgresVersion);
    assertEquals(
        Seq.of("test-0", "test-1")
        .append(Seq.range(2, cluster.getSpec().getShards().getClusters() + 1)
            .map(index -> BackupStorageUtil.getPath(
                cluster.getMetadata().getNamespace(),
                StackGresShardedClusterForCitusUtil.getClusterName(cluster, index),
                postgresMajorVersion)))
        .toList(),
        actualCluster.getSpec().getConfigurations().getBackups().get(0).getPaths());
  }

  private StackGresShardedCluster mutate(StackGresShardedClusterReview review) {
    return mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));
  }
}
