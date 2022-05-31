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
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opentest4j.AssertionFailedError;

@ExtendWith(MockitoExtension.class)
class DefaultStorageMigrationMutatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedVersions().findFirst().get();

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

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
    review = JsonUtil.readFromJson("cluster_allow_requests/valid_creation.json",
        StackGresClusterReview.class);
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    backupConfig = JsonUtil.readFromJson("backup_config/default.json",
        StackGresBackupConfig.class);

    mutator = new DefaultBackupStorageMigratorMutator(JSON_MAPPER,
        backupConfigFinder, objectStorageFinder, objectStorageScheduler);
    mutator.init();
  }

  @Test
  void clusterWithBackupPath_shouldRemoveDeprecatedBackupConfig() {
    StackGresCluster actualCluster = mutate(review);

    StackGresCluster expected = review.getRequest().getObject();
    expected.getSpec().getConfiguration().setBackupConfig(null);
    expected.getSpec().getConfiguration().setBackupPath(null);

    assertEquals(expected, actualCluster);
  }

  @Test
  void clusterWithBackupPath_shouldCopyIt() {
    review.getRequest().getObject().getSpec().getConfiguration().setBackupPath("demo/path/14");
    final StackGresCluster actualCluster = mutate(review);
    assertEquals("demo/path/14",
        actualCluster.getSpec().getConfiguration().getBackups().get(0).getPath());
  }

  @Test
  void clusterWithBackupConfig_shouldCopyIt() {
    review.getRequest().getObject().getSpec().getConfiguration().setBackupConfig("respaldosConfig");
    final StackGresCluster actualCluster = mutate(review);
    assertEquals("respaldosConfig",
        actualCluster.getSpec().getConfiguration().getBackups().get(0).getObjectStorage());
  }

  @Test
  void clusterWithBackupsPath_shouldSetNothing() {
    review.getRequest().getObject().getSpec().getConfiguration().setBackupConfig(null);
    review.getRequest().getObject().getSpec().getConfiguration().setBackupPath(null);
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups()
        .add(new StackGresClusterBackupConfiguration());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups()
        .get(0).setPath("test");
    StackGresCluster actualCluster = mutate(review);

    assertEquals(review.getRequest().getObject(), actualCluster);
  }

  @Test
  void clusterWithoutBackupsPath_shouldSetIt() {
    when(backupConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backupConfig));
    review.getRequest().getObject().getSpec().getConfiguration().setBackupPath(null);
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(null);
    final StackGresCluster actualCluster = mutate(review);

    final StackGresCluster cluster = review.getRequest().getObject();
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster).findMajorVersion(postgresVersion);
    assertEquals(
        BackupStorageUtil.getPath(
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(),
            postgresMajorVersion),
        actualCluster.getSpec().getConfiguration().getBackups().get(0).getPath());
  }

  @Test
  void oldClusterWithoutBackupPath_shouldSetItWithPreviousVersion() {
    final StackGresCluster cluster = review.getRequest().getObject();

    cluster.getSpec().getPostgres().setVersion("13.4");
    cluster.getMetadata().setAnnotations(new HashMap<>());
    cluster.getMetadata().getAnnotations().put(StackGresContext.VERSION_KEY, "1.1");
    cluster.getSpec().getConfiguration().setBackupPath(null);
    final StackGresCluster actualCluster = mutate(review);

    assertEquals(
        BackupStorageUtil.getPath(cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(), "13"),
        actualCluster.getSpec().getConfiguration().getBackups().get(0).getPath());
  }

  private StackGresCluster mutate(StackGresClusterReview review) {
    try {
      List<JsonPatchOperation> operations = mutator.mutate(review);
      JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());
      JsonNode newConfig = new JsonPatch(operations).apply(crJson);
      return JSON_MAPPER.treeToValue(newConfig, StackGresCluster.class);
    } catch (JsonPatchException | JsonProcessingException | IllegalArgumentException e) {
      throw new AssertionFailedError(e.getMessage(), e);
    }
  }
}
