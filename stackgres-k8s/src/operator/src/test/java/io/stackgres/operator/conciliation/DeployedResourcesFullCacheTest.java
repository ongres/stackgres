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

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeployedResourcesFullCacheTest {

  @Mock
  OperatorPropertyContext propertyContext;

  @Mock
  Metrics metrics;

  private DeployedResourcesFullCache cache;

  private StackGresShardedCluster generator;

  @BeforeEach
  void setUp() {
    cache = new DeployedResourcesFullCache(propertyContext, JsonUtil.jsonMapper(), metrics);
    generator = Fixtures.shardedCluster().loadDefault().get();
  }

  @Test
  void whenRequiredCustomResourceIsTheSame_shouldNotBeChanged() {
    StackGresCluster required = cluster();
    cache.put(generator, required, deployed(required));

    DeployedResourcesSnapshot snapshot = snapshot();
    StackGresCluster newRequired = cluster();

    assertFalse(snapshot.isChanged(newRequired, snapshot.get(newRequired)));
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
    annotations.put(StackGresContext.CITUS_GROUP_REGISTERED_ANNOTATION, "1025");
    newRequired.getMetadata().setAnnotations(annotations);

    assertTrue(snapshot.isChanged(newRequired, snapshot.get(newRequired)));
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

    assertTrue(snapshot.isChanged(newRequired, snapshot.get(newRequired)));
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
