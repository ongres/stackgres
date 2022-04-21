/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.StringUtil;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DbOpsJobComparatorTest {

  private final DbOpsJobComparator comparator = new DbOpsJobComparator();

  private Job required;
  private Job deployed;

  @BeforeEach
  void setUp() {
    required = JsonUtil.readFromJson("jobs/required.json",
        Job.class);
    deployed = JsonUtil.readFromJson("jobs/deployed.json",
        Job.class);
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
  void podAnnotationChanges_shouldBeDetected() {
    required.getSpec().getTemplate().getMetadata().setAnnotations(
        ImmutableMap.of(StringUtil.generateRandom(), StringUtil.generateRandom()));

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertFalse(isContentEqual);
  }

  @Test
  void containerImageChanges_shouldHaveNoDifference() {
    required.getSpec().getTemplate().getSpec().getContainers()
        .get(0).setImage("docker.io/ongres/patroni:v1.6.5-pg12.3-build-5.2");

    deployed.getSpec().getTemplate().getSpec().getContainers()
        .get(0).setImage("docker.io/ongres/patroni:v1.6.5-pg12.3-build-5.1");

    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertTrue(isContentEqual);
  }

}
