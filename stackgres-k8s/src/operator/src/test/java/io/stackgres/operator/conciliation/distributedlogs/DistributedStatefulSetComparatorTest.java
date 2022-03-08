/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.StringUtil;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DistributedStatefulSetComparatorTest {

  private final DistributedLogsStatefulSetComparator comparator =
      new DistributedLogsStatefulSetComparator();

  private StatefulSet required;
  private StatefulSet deployed;

  @BeforeEach
  void setUp() {
    required = Fixtures.statefulSet().loadRequiredDistributedLogs().get();
    deployed = Fixtures.statefulSet().loadDeployedDistributedLogs().get();
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
    required.getSpec().getTemplate().getSpec().getContainers()
        .get(0).setImage("docker.io/ongres/patroni:v1.6.5-pg12.3-build-5.2");

    deployed.getSpec().getTemplate().getSpec().getContainers()
        .get(0).setImage("docker.io/ongres/patroni:v1.6.5-pg12.3-build-5.1");

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertFalse(isContentEqual);
  }
}
