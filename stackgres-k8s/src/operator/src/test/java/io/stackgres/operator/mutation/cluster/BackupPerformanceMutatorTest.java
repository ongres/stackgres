/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupPerformance;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BackupPerformanceMutatorTest {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  private StackGresClusterReview review;
  private BackupPerformanceMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();

    mutator = new BackupPerformanceMutator();
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
    return mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));
  }
}
