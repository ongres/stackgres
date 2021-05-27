/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.ImmutableStackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DbOpsJobsGeneratorTest {

  @Inject
  @OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
  DbOpsJobsGenerator dbOpsJobsGenerator;

  StackGresCluster cluster;

  StackGresPostgresConfig clusterPgConfig;

  StackGresProfile clusterProfile;

  StackGresDbOps dbOps;


  @BeforeEach
  void setUp() {
    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);

    clusterPgConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);

    clusterProfile = JsonUtil.readFromJson("stackgres_profiles/size-xs.json",
        StackGresProfile.class);

    dbOps = JsonUtil.readFromJson("stackgres_dbops/dbops_securityupgrade.json",
        StackGresDbOps.class);
  }

  @Test
  void givenAContextWithASingleDbOpsWithoutRunAt_itShouldGenerateAJob() {

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
        .source(cluster)
        .postgresConfig(clusterPgConfig)
        .stackGresProfile(clusterProfile)
        .internalScripts(Seq.of())
        .addDbOps(dbOps)
        .build();

    dbOps.getSpec().setRunAt(null);
    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.stream().filter(r -> r.getKind().equals("Job"))
        .count());
  }

  @Test
  void givenAContextWithNoDbOps_itShouldNotGenerateAJob() {

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
        .source(cluster)
        .postgresConfig(clusterPgConfig)
        .stackGresProfile(clusterProfile)
        .internalScripts(Seq.of())
        .build();

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(0, generatedResources.size());
  }

  @Test
  void givenAContextWithADbOpsWithAPastRunAt_shouldGenerateAJob() {

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
        .source(cluster)
        .postgresConfig(clusterPgConfig)
        .stackGresProfile(clusterProfile)
        .internalScripts(Seq.of())
        .addDbOps(dbOps)
        .build();

    dbOps.getSpec().setRunAt(Instant.now().minusMillis(1000).toString());

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.size());
  }

  @Test
  void givenAContextWithADbOpsWithAFutureRunAt_shouldNotGenerateAJob() {

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
        .source(cluster)
        .postgresConfig(clusterPgConfig)
        .stackGresProfile(clusterProfile)
        .internalScripts(Seq.of())
        .addDbOps(dbOps)
        .build();

    dbOps.getSpec().setRunAt(Instant.now().plusMillis(1000).toString());

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(0, generatedResources.size());

  }
}