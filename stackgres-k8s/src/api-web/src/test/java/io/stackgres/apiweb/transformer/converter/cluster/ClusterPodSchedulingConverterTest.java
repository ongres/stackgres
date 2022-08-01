/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.converter.cluster;

import static org.junit.Assert.assertEquals;

import io.stackgres.apiweb.dto.cluster.ClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClusterPodSchedulingConverterTest {

  StackGresClusterPodScheduling stackGresPodSchedulingFixture;

  ClusterPodSchedulingConverter converter;

  @BeforeEach
  public void setup() {
    this.converter = new ClusterPodSchedulingConverter();
    this.stackGresPodSchedulingFixture = Fixtures.cluster().scheduling().loadDefault().get();
  }

  @Test
  void shouldClusterPodScheduling_hasPreferredSchedulingBehaviorAfterConversion() {
    ClusterPodScheduling podSchedulingDto = converter.from(stackGresPodSchedulingFixture);

    assertEquals(
        stackGresPodSchedulingFixture.getNodeAffinity()
            .getPreferredDuringSchedulingIgnoredDuringExecution(),
        podSchedulingDto.getNodeAffinity().getPreferredDuringSchedulingIgnoredDuringExecution());
  }

  @Test
  void shouldClusterPodScheduling_hasRequiredSchedulingBehaviorAfterConversion() {
    ClusterPodScheduling podSchedulingDto = converter.from(stackGresPodSchedulingFixture);

    assertEquals(
        stackGresPodSchedulingFixture.getNodeAffinity()
            .getRequiredDuringSchedulingIgnoredDuringExecution(),
        podSchedulingDto.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution());
  }
}
