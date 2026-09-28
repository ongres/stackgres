/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeployedResourcesCacheTest {

  @Mock
  OperatorPropertyContext propertyContext;

  private DeployedResourcesCache cache;

  private StackGresShardedCluster generator;

  @BeforeEach
  void setUp() {
    cache = new DeployedResourcesCache(propertyContext, JsonUtil.jsonMapper());
    generator = Fixtures.shardedCluster().loadDefault().get();
  }

  @Test
  void whenRequiredCustomResourceIsTheSame_shouldNotBeChanged() {
    StackGresCluster required = cluster();
    cache.put(generator, required, deployed(required));

    DeployedResourcesSnapshot snapshot = snapshot();
    StackGresCluster newRequired = cluster();

    assertFalse(snapshot.isRequiredChanged(newRequired));
  }

  @Test
  void whenRequiredCustomResourceAnnotationsChange_shouldBeChanged() {
    StackGresCluster required = cluster();
    cache.put(generator, required, deployed(required));

    DeployedResourcesSnapshot snapshot = snapshot();
    StackGresCluster newRequired = cluster();
    var annotations = new HashMap<>(Optional
        .ofNullable(newRequired.getMetadata().getAnnotations())
        .orElse(new HashMap<>()));
    annotations.put("test", "test");
    newRequired.getMetadata().setAnnotations(annotations);

    assertTrue(snapshot.isRequiredChanged(newRequired));
  }

  @Test
  void whenRequiredCustomResourceLabelsChange_shouldBeChanged() {
    StackGresCluster required = cluster();
    cache.put(generator, required, deployed(required));

    DeployedResourcesSnapshot snapshot = snapshot();
    StackGresCluster newRequired = cluster();
    var labels = new HashMap<>(Optional
        .ofNullable(newRequired.getMetadata().getLabels())
        .orElse(new HashMap<>()));
    labels.put("test", "test");
    newRequired.getMetadata().setLabels(labels);

    assertTrue(snapshot.isRequiredChanged(newRequired));
  }

  private StackGresCluster cluster() {
    StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setNamespace(generator.getMetadata().getNamespace());
    // As done by AbstractConciliator for any required resource
    cluster.getMetadata().setManagedFields(null);
    cluster.setStatus(null);
    return cluster;
  }

  private StackGresCluster deployed(StackGresCluster required) {
    StackGresCluster deployed = JsonUtil.copy(required);
    deployed.getMetadata().setResourceVersion("1");
    return deployed;
  }

  private DeployedResourcesSnapshot snapshot() {
    StackGresCluster foundDeployed = deployed(cluster());
    return cache.createDeployedResourcesSnapshot(
        generator, List.of(foundDeployed), List.of(foundDeployed));
  }

}
