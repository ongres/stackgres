/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultStorageMigrationMutatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  @Mock
  CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;
  @Mock
  CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;
  @Mock
  CustomResourceScheduler<StackGresObjectStorage> objectStorageScheduler;

  private StackGresClusterReview review;
  private StackGresBackupConfig backupConfig;
  private DefaultBackupStorageMigratorMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    backupConfig = Fixtures.backupConfig().loadDefault().get();

    mutator = new DefaultBackupStorageMigratorMutator(
        backupConfigFinder, objectStorageFinder, objectStorageScheduler);
  }

  @Test
  void clusterWithBackupPath_shouldRemoveDeprecatedBackupConfig() {
    review.getRequest().getObject().getSpec().getConfigurations().setBackups(null);

    StackGresCluster actualCluster = mutate(review);

    StackGresCluster expected = review.getRequest().getObject();
    expected.getSpec().getConfigurations().setSgBackupConfig(null);
    expected.getSpec().getConfigurations().setBackupPath(null);

    assertEquals(expected, actualCluster);
  }

  @Test
  void clusterWithBackupPath_shouldCopyIt() {
    when(backupConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backupConfig));
    review.getRequest().getObject().getSpec().getConfigurations().setBackupPath("demo/path/14");
    final StackGresCluster actualCluster = mutate(review);
    assertEquals("demo/path/14",
        actualCluster.getSpec().getConfigurations().getBackups().get(0).getPath());
  }

  @Test
  void clusterWithBackupConfig_shouldCopyIt() {
    when(backupConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backupConfig));
    review.getRequest().getObject().getSpec().getConfigurations()
        .setSgBackupConfig("respaldosConfig");
    final StackGresCluster actualCluster = mutate(review);
    assertEquals("respaldosConfig",
        actualCluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage());
  }

  @Test
  void clusterWithBackupsPath_shouldSetNothing() {
    review.getRequest().getObject().getSpec().getConfigurations().setSgBackupConfig(null);
    review.getRequest().getObject().getSpec().getConfigurations().setBackupPath(null);
    review.getRequest().getObject().getSpec().getConfigurations().setBackups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getConfigurations().getBackups()
        .add(new StackGresClusterBackupConfiguration());
    review.getRequest().getObject().getSpec().getConfigurations().getBackups()
        .get(0).setPath("test");
    StackGresCluster actualCluster = mutate(review);

    assertEquals(review.getRequest().getObject(), actualCluster);
  }

  @Test
  void clusterWithoutBackupsPath_shouldSetIt() {
    when(backupConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backupConfig));
    review.getRequest().getObject().getSpec().getConfigurations().setBackupPath(null);
    review.getRequest().getObject().getSpec().getConfigurations().setBackups(null);
    final StackGresCluster actualCluster = mutate(review);

    final StackGresCluster cluster = review.getRequest().getObject();
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster).getMajorVersion(postgresVersion);
    assertEquals(
        BackupStorageUtil.getPath(
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(),
            postgresMajorVersion),
        actualCluster.getSpec().getConfigurations().getBackups().get(0).getPath());
  }

  @Test
  void oldClusterWithoutBackupPath_shouldSetItWithPreviousVersion() {
    when(backupConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backupConfig));

    final StackGresCluster cluster = review.getRequest().getObject();

    cluster.getSpec().getPostgres().setVersion("13.12");
    cluster.getMetadata().setAnnotations(new HashMap<>());
    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    cluster.getSpec().getConfigurations().setBackupPath(null);
    final StackGresCluster actualCluster = mutate(review);

    assertEquals(
        BackupStorageUtil.getPath(cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(), "13"),
        actualCluster.getSpec().getConfigurations().getBackups().get(0).getPath());
  }

  private StackGresCluster mutate(StackGresClusterReview review) {
    return mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));
  }
}
