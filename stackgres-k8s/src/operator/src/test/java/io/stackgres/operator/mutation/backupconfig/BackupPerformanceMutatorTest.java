/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backupconfig;

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
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class BackupPerformanceMutatorTest {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  private BackupConfigReview review;
  private BackupPerformanceMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = JsonUtil.readFromJson("backupconfig_allow_request/create.json",
        BackupConfigReview.class);

    mutator = new BackupPerformanceMutator();
    mutator.init();
  }

  @Test
  void clusterWithNonDeprecatedValues_shouldSetNothing() {
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxNetworkBandwidth(2L);

    StackGresBackupConfig actualBackupConfig = mutate(review);

    assertEquals(1L, actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithNullAndDeprecatedMaxDiskBandwidthValue_shouldMoveValueToTheOtherField() {
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxDiskBandwitdh(1L);
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxNetworkBandwidth(2L);

    StackGresBackupConfig actualBackupConfig = mutate(review);

    assertEquals(1L, actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithNullAndDeprecatedMaxNetworkBandwidthValue_shouldMoveValueToTheOtherField() {
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxNetworkBandwitdh(2L);

    StackGresBackupConfig actualBackupConfig = mutate(review);

    assertEquals(1L, actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithDeprecatedMaxDiskBandwidthValue_shouldSetValueToNull() {
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxDiskBandwitdh(3L);
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxNetworkBandwidth(2L);

    StackGresBackupConfig actualBackupConfig = mutate(review);

    assertEquals(1L, actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithDeprecatedMaxNetworkBandwidthValue_shouldSetValueToNull() {
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxNetworkBandwidth(2L);
    review.getRequest().getObject().getSpec().getBaseBackups()
        .getPerformance().setMaxNetworkBandwitdh(4L);

    StackGresBackupConfig actualBackupConfig = mutate(review);

    assertEquals(1L, actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualBackupConfig.getSpec().getBaseBackups()
        .getPerformance().getMaxNetworkBandwitdh());
  }

  private StackGresBackupConfig mutate(BackupConfigReview review) {
    try {
      List<JsonPatchOperation> operations = mutator.mutate(review);
      JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());
      JsonNode newConfig = new JsonPatch(operations).apply(crJson);
      return JSON_MAPPER.treeToValue(newConfig, StackGresBackupConfig.class);
    } catch (JsonPatchException | JsonProcessingException | IllegalArgumentException e) {
      throw new AssertionFailedError(e.getMessage(), e);
    }
  }
}
