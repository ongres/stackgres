/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opentest4j.AssertionFailedError;

@ExtendWith(MockitoExtension.class)
class DefaultBackupPathMutatorTest {

  private static final String POSTGRES_VERSION = "14.4";

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresClusterReview review;
  private DefaultBackupPathMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    mutator = new DefaultBackupPathMutator();
    mutator.init();
  }

  @Test
  void clusterWithBackupPath_shouldSetNothing() {
    StackGresCluster actualCluster = mutate(review);
    assertEquals(review.getRequest().getObject(), actualCluster);
  }

  @Test
  void clusterWithoutBackupPath_shouldSetIt() {
    final StackGresCluster cluster = review.getRequest().getObject();
    cluster.getMetadata().setAnnotations(Map.of(StackGresContext.VERSION_KEY, "1.2"));
    cluster.getSpec().getConfiguration().setBackupPath(null);
    var backupConfiguration = new StackGresClusterBackupConfiguration();
    backupConfiguration.setObjectStorage("backupconf");
    cluster.getSpec().getConfiguration().setBackups(List.of(backupConfiguration));

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
            postgresMajorVersion),
        actualCluster.getSpec().getConfiguration().getBackups().get(0).getPath());
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
