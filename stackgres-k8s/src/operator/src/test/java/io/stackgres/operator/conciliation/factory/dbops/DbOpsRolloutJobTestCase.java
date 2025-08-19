/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

abstract class DbOpsRolloutJobTestCase {

  @Inject
  @OperatorVersionBinder
  DbOpsClusterRollout dbOpsClusterRollout;

  StackGresConfig config;

  StackGresCluster cluster;

  StackGresDbOps dbOps;

  StackGresProfile clusterProfile;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    cluster = Fixtures.cluster().loadDefault().get();
    clusterProfile = Fixtures.instanceProfile().loadSizeS().get();
    clusterProfile = Fixtures.instanceProfile().loadSizeS().get();
    dbOps = getDbOps();
  }

  abstract StackGresDbOps getDbOps();

  void setSgDbOpsScheduling() {
    var dbopsScheduling = Fixtures.dbOps().scheduling().loadDefault().get();
    dbOps.getSpec().setScheduling(dbopsScheduling);
  }

  @Test
  void givenAContextWithASingleDbOpsWithoutRunAt_itShouldGenerateACluster() {
    StackGresDbOpsContext context = StackGresDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundCluster(cluster)
        .foundProfile(clusterProfile)
        .foundClusterPods(List.of())
        .foundClusterPatroniMembers(List.of())
        .build();

    dbOps.getSpec().setRunAt(null);
    var generatedResources = dbOpsClusterRollout.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.stream().filter(r -> r.getKind().equals(StackGresCluster.KIND))
        .count());
  }

  @Test
  void givenAContextWithADbOpsWithAPastRunAt_shouldGenerateACluster() {
    StackGresDbOpsContext context = StackGresDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundCluster(cluster)
        .foundProfile(clusterProfile)
        .foundClusterPods(List.of())
        .foundClusterPatroniMembers(List.of())
        .build();

    dbOps.getSpec().setRunAt(Instant.now().minusMillis(1000).toString());

    var generatedResources = dbOpsClusterRollout.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.size());
  }

  @Test
  void givenAContextWithADbOpsWithAFutureRunAt_shouldNotGenerateACluster() {
    StackGresDbOpsContext context = StackGresDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .build();

    dbOps.getSpec().setRunAt(Instant.now().plusMillis(1000).toString());

    var generatedResources = dbOpsClusterRollout.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(0, generatedResources.size());
  }

}
