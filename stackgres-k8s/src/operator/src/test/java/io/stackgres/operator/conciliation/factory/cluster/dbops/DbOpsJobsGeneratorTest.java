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
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.ImmutableStackGresDbOpsContext;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.dbops.DbOpsJobsGenerator;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DbOpsJobsGeneratorTest {

  @Inject
  @OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V11)
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
    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(dbOps)
        .cluster(cluster)
        .build();

    dbOps.getSpec().setRunAt(null);
    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.stream().filter(r -> r.getKind().equals("Job"))
        .count());
  }

  @Test
  void givenAContextWithADbOpsWithAPastRunAt_shouldGenerateAJob() {
    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(dbOps)
        .cluster(cluster)
        .build();

    dbOps.getSpec().setRunAt(Instant.now().minusMillis(1000).toString());

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.size());
  }

  @Test
  void givenAContextWithADbOpsWithAFutureRunAt_shouldNotGenerateAJob() {
    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(dbOps)
        .cluster(cluster)
        .build();

    dbOps.getSpec().setRunAt(Instant.now().plusMillis(1000).toString());

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(0, generatedResources.size());
  }
}
