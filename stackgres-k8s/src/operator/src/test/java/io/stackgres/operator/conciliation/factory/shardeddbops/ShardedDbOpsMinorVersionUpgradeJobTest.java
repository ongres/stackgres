/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtensionBuilder;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class ShardedDbOpsMinorVersionUpgradeJobTest extends ShardedDbOpsJobTestCase {

  @Override
  StackGresShardedDbOps getShardedDbOps() {
    return Fixtures.shardedDbOps().loadMinorVersionUpgrade().get();
  }

  @Test
  void givenAMinorVersionUpgradeWithoutExtensions_shouldNotSetTheExtensions() {
    Map<String, String> env = getRunEnv();

    assertEquals("", env.get("TARGET_POSTGRES_EXTENSIONS_JSON"));
    assertEquals("", env.get("CLUSTER_POSTGRES_EXTENSIONS_JSON"));
  }

  @Test
  void givenAMinorVersionUpgradeWithExtensions_shouldSetTheExtensions() {
    cluster.getSpec().getPostgres().setExtensions(List.of(
        new StackGresClusterExtensionBuilder()
        .withName("timescaledb")
        .withVersion("1.0.0")
        .build(),
        new StackGresClusterExtensionBuilder()
        .withName("pg_repack")
        .build()));
    dbOps.getSpec().getMinorVersionUpgrade().setPostgresExtensions(List.of(
        new StackGresClusterExtensionBuilder()
        .withName("timescaledb")
        .withVersion("2.0.0")
        .build()));
    dbOps.getSpec().getMinorVersionUpgrade().setToInstallPostgresExtensions(List.of(
        new StackGresClusterInstalledExtensionBuilder()
        .withName("pg_repack")
        .withPublisher("com.ongres")
        .withVersion("1.5.0")
        .withRepository("https://extensions.stackgres.io/postgres/repository")
        .withPostgresVersion("17")
        .withBuild("6.58")
        .build(),
        new StackGresClusterInstalledExtensionBuilder()
        .withName("timescaledb")
        .withPublisher("com.ongres")
        .withVersion("2.0.0")
        .withRepository("https://extensions.stackgres.io/postgres/repository")
        .withPostgresVersion("17")
        .withBuild("6.58")
        .build()));

    Map<String, String> env = getRunEnv();

    assertEquals(
        "[{\"name\":\"pg_repack\"},{\"name\":\"timescaledb\",\"version\":\"2.0.0\"}]",
        env.get("TARGET_POSTGRES_EXTENSIONS_JSON"));
    assertEquals(
        "[{\"name\":\"pg_repack\",\"publisher\":\"com.ongres\",\"version\":\"1.5.0\","
            + "\"repository\":\"https://extensions.stackgres.io/postgres/repository\"},"
            + "{\"name\":\"timescaledb\",\"publisher\":\"com.ongres\",\"version\":\"2.0.0\","
            + "\"repository\":\"https://extensions.stackgres.io/postgres/repository\"}]",
        env.get("CLUSTER_POSTGRES_EXTENSIONS_JSON"));
  }

  private Map<String, String> getRunEnv() {
    StackGresShardedDbOpsContext context = StackGresShardedDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundShardedCluster(cluster)
        .foundCoordinator(coordinator)
        .foundProfile(clusterProfile)
        .build();

    dbOps.getSpec().setRunAt(null);
    var job = (Job) dbOpsJobsGenerator.generateResource(context)
        .filter(Job.class::isInstance)
        .findFirst()
        .orElseThrow();
    return Stream.concat(
        job.getSpec().getTemplate().getSpec().getInitContainers().stream(),
        job.getSpec().getTemplate().getSpec().getContainers().stream())
        .flatMap(container -> container.getEnv().stream())
        .filter(envVar -> envVar.getValue() != null)
        .collect(Collectors.toMap(EnvVar::getName, EnvVar::getValue, (first, last) -> first));
  }

}
