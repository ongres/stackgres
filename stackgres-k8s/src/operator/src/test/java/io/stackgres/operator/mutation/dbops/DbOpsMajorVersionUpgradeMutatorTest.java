/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opentest4j.AssertionFailedError;

@ExtendWith(MockitoExtension.class)
class DbOpsMajorVersionUpgradeMutatorTest {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  private DbOpsReview review;
  private StackGresCluster cluster;
  private DbOpsMajorVersionUpgradeMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.dbOps().loadMajorVersionUpgradeCreate().get();
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setNamespace(
        review.getRequest().getObject().getMetadata().getNamespace());

    mutator = new DbOpsMajorVersionUpgradeMutator();
    mutator.init();
    mutator.setBackupConfigFinder(clusterFinder);
  }

  @Test
  void majorVersionUpgradeWithBackupPath_shouldSetNothing() {
    StackGresDbOps actualDbOps = mutate(review);

    assertEquals(review.getRequest().getObject(), actualDbOps);
  }

  @Test
  void majorVersionUpgradeWithoutBackupPath_shouldSetNothing() {
    cluster.getSpec().getConfiguration().setBackupConfig(null);
    cluster.getSpec().getConfiguration().setBackupPath(null);
    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setBackupPath(null);
    final StackGresDbOps actualDbOps = mutate(review);

    assertEquals(review.getRequest().getObject(), actualDbOps);
  }

  @Test
  void majorVersionUpgradeWithBackupButWithoutBackupPath_shouldSetIt() {
    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setBackupPath(null);
    final StackGresDbOps actualDbOps = mutate(review);

    final StackGresDbOps dbOps = review.getRequest().getObject();
    final String postgresVersion = dbOps.getSpec()
        .getMajorVersionUpgrade().getPostgresVersion();
    final String postgresFlavor = cluster.getSpec()
        .getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .getMajorVersion(postgresVersion);
    assertEquals(
        BackupStorageUtil.getPath(
            dbOps.getMetadata().getNamespace(),
            dbOps.getSpec().getSgCluster(),
            postgresMajorVersion),
        actualDbOps.getSpec().getMajorVersionUpgrade().getBackupPath());
  }

  @Test
  void majorVersionUpgradeWithBackupsButWithoutBackupPath_shouldSetIt() {
    cluster.getSpec().getConfiguration().setBackupConfig(null);
    cluster.getSpec().getConfiguration().setBackupPath(null);
    cluster.getSpec().getConfiguration().setBackups(new ArrayList<>());
    cluster.getSpec().getConfiguration().getBackups()
        .add(new StackGresClusterBackupConfiguration());
    cluster.getSpec().getConfiguration().getBackups()
        .get(0).setObjectStorage("test");
    cluster.getSpec().getConfiguration().getBackups()
        .get(0).setPath("test");
    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setBackupPath(null);
    final StackGresDbOps actualDbOps = mutate(review);

    final StackGresDbOps dbOps = review.getRequest().getObject();
    final String postgresVersion = dbOps.getSpec()
        .getMajorVersionUpgrade().getPostgresVersion();
    final String postgresFlavor = cluster.getSpec()
        .getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .getMajorVersion(postgresVersion);
    assertEquals(
        BackupStorageUtil.getPath(
            dbOps.getMetadata().getNamespace(),
            dbOps.getSpec().getSgCluster(),
            postgresMajorVersion),
        actualDbOps.getSpec().getMajorVersionUpgrade().getBackupPath());
  }

  private StackGresDbOps mutate(DbOpsReview review) {
    try {
      List<JsonPatchOperation> operations = mutator.mutate(review);
      JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());
      JsonNode newConfig = new JsonPatch(operations).apply(crJson);
      return JSON_MAPPER.treeToValue(newConfig, StackGresDbOps.class);
    } catch (JsonPatchException | JsonProcessingException | IllegalArgumentException e) {
      throw new AssertionFailedError(e.getMessage(), e);
    }
  }
}
