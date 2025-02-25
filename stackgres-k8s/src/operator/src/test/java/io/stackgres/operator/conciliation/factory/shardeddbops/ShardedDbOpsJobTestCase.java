/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

abstract class ShardedDbOpsJobTestCase {

  @Inject
  @OperatorVersionBinder
  ShardedDbOpsJobsGenerator dbOpsJobsGenerator;

  StackGresConfig config;

  StackGresShardedCluster cluster;

  StackGresCluster coordinator;

  StackGresShardedDbOps dbOps;

  StackGresProfile clusterProfile;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();

    cluster = Fixtures.shardedCluster().loadDefault().get();

    coordinator = Fixtures.cluster().loadDefault().get();

    clusterProfile = Fixtures.instanceProfile().loadSizeS().get();

    dbOps = getShardedDbOps();
  }

  abstract StackGresShardedDbOps getShardedDbOps();

  void setSgShardedDbOpsScheduling() {
    var dbopsScheduling = Fixtures.shardedDbOps().scheduling().loadDefault().get();
    dbOps.getSpec().setScheduling(dbopsScheduling);
  }

  @Test
  void givenAContextWithASingleDbOpsWithoutRunAt_itShouldGenerateAJob() {
    StackGresShardedDbOpsContext context = StackGresShardedDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundShardedCluster(cluster)
        .foundCoordinator(coordinator)
        .foundProfile(clusterProfile)
        .build();

    dbOps.getSpec().setRunAt(null);
    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.stream().filter(r -> r.getKind().equals("Job"))
        .count());
  }

  @Test
  void givenAContextWithADbOpsWithAPastRunAt_shouldGenerateAJob() {
    StackGresShardedDbOpsContext context = StackGresShardedDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundShardedCluster(cluster)
        .foundCoordinator(coordinator)
        .foundProfile(clusterProfile)
        .build();

    dbOps.getSpec().setRunAt(Instant.now().minusMillis(1000).toString());

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.size());
  }

  @Test
  void givenAContextWithADbOpsWithAFutureRunAt_shouldNotGenerateAJob() {
    StackGresShardedDbOpsContext context = StackGresShardedDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundShardedCluster(cluster)
        .foundCoordinator(coordinator)
        .foundProfile(clusterProfile)
        .build();

    dbOps.getSpec().setRunAt(Instant.now().plusMillis(1000).toString());

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(0, generatedResources.size());
  }

  @Test
  void shouldGenerateJobWithNodeSelector_onceSgDbOpsSchedulingHasNodeSelector() {
    dbOps.getSpec().setRunAt(null);
    setSgShardedDbOpsScheduling();

    StackGresShardedDbOpsContext context = StackGresShardedDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundShardedCluster(cluster)
        .foundCoordinator(coordinator)
        .foundProfile(clusterProfile)
        .build();

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());
    var job = (Job) generatedResources.getFirst();
    assertEquals(2, job.getSpec().getTemplate().getSpec().getNodeSelector().size());

  }

  @Test
  void shouldGenerateJobWithNodeAffinity_onceSgDbOpsSchedulingHasAffinity() {
    dbOps.getSpec().setRunAt(null);
    setSgShardedDbOpsScheduling();

    StackGresShardedDbOpsContext context = StackGresShardedDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundShardedCluster(cluster)
        .foundCoordinator(coordinator)
        .foundProfile(clusterProfile)
        .build();

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    var job = (Job) generatedResources.getFirst();
    var nodeAffinity = job.getSpec().getTemplate().getSpec().getAffinity().getNodeAffinity();
    assertEquals(1, nodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution().size());
    assertEquals(1, nodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution()
        .getNodeSelectorTerms().size());
  }

}
