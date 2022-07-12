/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

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
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupPerformance;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class BackupPerformanceMutatorTest {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  private BackupReview review;
  private BackupPerformanceMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = JsonUtil.readFromJson("backup_allow_request/create.json",
        BackupReview.class);

    mutator = new BackupPerformanceMutator();
    mutator.init();
  }

  @Test
  void clusterWithNonDeprecatedValues_shouldSetNothing() {
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .setPerformance(new StackGresBaseBackupPerformance());
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxNetworkBandwidth(2L);

    StackGresBackup actualBackupConfig = mutate(review);

    assertEquals(1L, actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithNullAndDeprecatedMaxDiskBandwidthValue_shouldMoveValueToTheOtherField() {
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .setPerformance(new StackGresBaseBackupPerformance());
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxDiskBandwitdh(1L);
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxNetworkBandwidth(2L);

    StackGresBackup actualBackupConfig = mutate(review);

    assertEquals(1L, actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithNullAndDeprecatedMaxNetworkBandwidthValue_shouldMoveValueToTheOtherField() {
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .setPerformance(new StackGresBaseBackupPerformance());
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxNetworkBandwitdh(2L);

    StackGresBackup actualBackupConfig = mutate(review);

    assertEquals(1L, actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithDeprecatedMaxDiskBandwidthValue_shouldSetValueToNull() {
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .setPerformance(new StackGresBaseBackupPerformance());
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxDiskBandwitdh(3L);
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxNetworkBandwidth(2L);

    StackGresBackup actualBackupConfig = mutate(review);

    assertEquals(1L, actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxNetworkBandwitdh());
  }

  @Test
  void clusterWithDeprecatedMaxNetworkBandwidthValue_shouldSetValueToNull() {
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .setPerformance(new StackGresBaseBackupPerformance());
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxDiskBandwidth(1L);
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxNetworkBandwidth(2L);
    review.getRequest().getObject().getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().setMaxNetworkBandwitdh(4L);

    StackGresBackup actualBackupConfig = mutate(review);

    assertEquals(1L, actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxDiskBandwidth());
    assertEquals(2L, actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxNetworkBandwidth());
    assertNull(actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxDiskBandwitdh());
    assertNull(actualBackupConfig.getStatus().getBackupConfig().getBaseBackups()
        .getPerformance().getMaxNetworkBandwitdh());
  }

  private StackGresBackup mutate(BackupReview review) {
    try {
      List<JsonPatchOperation> operations = mutator.mutate(review);
      JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());
      JsonNode newConfig = new JsonPatch(operations).apply(crJson);
      return JSON_MAPPER.treeToValue(newConfig, StackGresBackup.class);
    } catch (JsonPatchException | JsonProcessingException | IllegalArgumentException e) {
      throw new AssertionFailedError(e.getMessage(), e);
    }
  }
}
