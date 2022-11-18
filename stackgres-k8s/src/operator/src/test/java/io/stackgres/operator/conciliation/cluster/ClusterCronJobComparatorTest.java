/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.stackgres.common.StringUtil;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClusterCronJobComparatorTest {

  private final ClusterCronJobComparator comparator = new ClusterCronJobComparator();

  private CronJob required;
  private CronJob deployed;

  @BeforeEach
  void setUp() {
    required = Fixtures.cronJob().loadRequired().get();
    deployed = Fixtures.cronJob().loadDeployed().get();
  }

  @Test
  void generatedResourceAndRequiredResource_shouldHaveNoDifference() {

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertTrue(isContentEqual);
  }

  @Test
  void annotationChanges_shouldBeDetected() {
    required.getMetadata().setAnnotations(
        ImmutableMap.of(StringUtil.generateRandom(), StringUtil.generateRandom()));

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertFalse(isContentEqual);
  }

  @Test
  void containerImageChanges_shouldBeDetected() {
    required.getSpec().setSchedule("0 5 31 2 *");

    deployed.getSpec().setSchedule("*/30 * * * *");

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertFalse(isContentEqual);
  }

}
