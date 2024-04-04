/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultBackupPathMutatorTest {

  private static final String POSTGRES_VERSION = "14.4";

  private StackGresClusterReview review;
  private DefaultBackupPathMutator mutator;
  private Instant defaultTimestamp;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    defaultTimestamp = Instant.now();
    mutator = new DefaultBackupPathMutator(defaultTimestamp);
  }

  @Test
  void clusterWithBackupPath_shouldSetNothing() {
    StackGresCluster actualCluster = mutate(review);
    assertEquals(review.getRequest().getObject(), actualCluster);
  }

  @Test
  void clusterWithoutBackupPath_shouldSetIt() {
    final StackGresCluster cluster = review.getRequest().getObject();
    cluster.getMetadata().setAnnotations(
        Map.of(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion()));
    var backupConfiguration = new StackGresClusterBackupConfiguration();
    backupConfiguration.setSgObjectStorage("backupconf");
    cluster.getSpec().getConfigurations().setBackups(List.of(backupConfiguration));

    final StackGresCluster actualCluster = mutate(review);

    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .getMajorVersion(postgresVersion);
    assertEquals(
        BackupStorageUtil.getPath(
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(),
            defaultTimestamp,
            postgresMajorVersion),
        actualCluster.getSpec().getConfigurations().getBackups().get(0).getPath());
  }

  @Test
  void clusterWithBackupsPath_shouldSetNothing() {
    review.getRequest().getObject().getSpec().getConfigurations().setBackups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getConfigurations().getBackups()
        .add(new StackGresClusterBackupConfiguration());
    review.getRequest().getObject().getSpec().getConfigurations().getBackups()
        .get(0).setPath("test");
    StackGresCluster actualCluster = mutate(review);

    assertEquals(review.getRequest().getObject(), actualCluster);
  }

  private StackGresCluster mutate(StackGresClusterReview review) {
    return mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));
  }
}
