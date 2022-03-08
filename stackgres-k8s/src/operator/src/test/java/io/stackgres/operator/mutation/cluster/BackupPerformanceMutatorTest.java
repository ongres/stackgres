/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupPerformance;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class BackupPerformanceMutatorTest {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  private StackGresClusterReview review;
  private BackupPerformanceMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();

    mutator = new BackupPerformanceMutator();
    mutator.init();
  }

  @Test
  void clusterWithNonDeprecatedValues_shouldSetNothing() {
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(
        List.of(new StackGresClusterBackupConfiguration()));
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .setPerformance(new StackGresBaseBackupPerformance());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxNetworkBandwidth(2L);

    StackGresCluster actualCluster = mutate(review);

    assertEquals(1L, actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithNullAndDeprecatedMaxDiskBandwidthValue_shouldMoveValueToTheOtherField() {
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(
        List.of(new StackGresClusterBackupConfiguration()));
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .setPerformance(new StackGresBaseBackupPerformance());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxDiskBandwitdh(1L);
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxNetworkBandwidth(2L);

    StackGresCluster actualCluster = mutate(review);

    assertEquals(1L, actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithNullAndDeprecatedMaxNetworkBandwidthValue_shouldMoveValueToTheOtherField() {
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(
        List.of(new StackGresClusterBackupConfiguration()));
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .setPerformance(new StackGresBaseBackupPerformance());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxNetworkBandwitdh(2L);

    StackGresCluster actualCluster = mutate(review);

    assertEquals(1L, actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithDeprecatedMaxDiskBandwidthValue_shouldSetValueToNull() {
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(
        List.of(new StackGresClusterBackupConfiguration()));
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .setPerformance(new StackGresBaseBackupPerformance());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxDiskBandwitdh(3L);
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxNetworkBandwidth(2L);

    StackGresCluster actualCluster = mutate(review);

    assertEquals(1L, actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithDeprecatedMaxNetworkBandwidthValue_shouldSetValueToNull() {
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(
        List.of(new StackGresClusterBackupConfiguration()));
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .setPerformance(new StackGresBaseBackupPerformance());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxNetworkBandwidth(2L);
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().setMaxNetworkBandwitdh(4L);

    StackGresCluster actualCluster = mutate(review);

    assertEquals(1L, actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualCluster.getSpec().getConfiguration().getBackups().get(0)
        .getPerformance().getMaxNetworkBandwitdh());
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
