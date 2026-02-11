/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForDbOps;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgbenchBenchmarkTest {

  @Mock
  private ResourceFactory<StackGresDbOpsContext, PodSecurityContext> podSecurityFactory;

  @Mock
  private DbOpsEnvironmentVariables clusterStatefulSetEnvironmentVariables;

  @Mock
  private LabelFactoryForCluster labelFactory;

  @Mock
  private LabelFactoryForDbOps dbOpsLabelFactory;

  @Mock
  private KubectlUtil kubectl;

  @Mock
  private DbOpsVolumeMounts dbOpsVolumeMounts;

  @Mock
  private DbOpsTemplatesVolumeFactory dbOpsTemplatesVolumeFactory;

  @Mock
  private StackGresDbOpsContext context;

  private PgbenchBenchmark pgbenchBenchmark;

  private StackGresDbOps dbOps;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    ObjectMapper jsonMapper = new ObjectMapper();
    pgbenchBenchmark = new PgbenchBenchmark(
        podSecurityFactory,
        clusterStatefulSetEnvironmentVariables,
        labelFactory,
        dbOpsLabelFactory,
        jsonMapper,
        kubectl,
        dbOpsVolumeMounts,
        dbOpsTemplatesVolumeFactory);

    dbOps = Fixtures.dbOps().loadPgbench().get();
    cluster = Fixtures.cluster().loadDefault().get();

    lenient().when(context.getSource()).thenReturn(dbOps);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getSamplingStatus()).thenReturn(Optional.empty());
    lenient().when(dbOpsLabelFactory.dbOpsPodLabels(dbOps)).thenReturn(Map.of());
    lenient().when(kubectl.getImageName(cluster)).thenReturn("kubectl:latest");
  }

  @Test
  void createJob_whenPgbenchOp_shouldCreateJobWithPgbenchEnvVars() {
    Job job = pgbenchBenchmark.createJob(context);

    assertNotNull(job);
    assertEquals("Job", job.getKind());

    List<EnvVar> runContainerEnvVars = job.getSpec().getTemplate().getSpec()
        .getContainers().stream()
        .filter(c -> "run-dbops".equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getEnv();

    assertTrue(runContainerEnvVars.stream()
        .anyMatch(env -> "PGHOST".equals(env.getName())));
    assertTrue(runContainerEnvVars.stream()
        .anyMatch(env -> "SCALE".equals(env.getName())));
    assertTrue(runContainerEnvVars.stream()
        .anyMatch(env -> "DURATION".equals(env.getName())));
    assertTrue(runContainerEnvVars.stream()
        .anyMatch(env -> "SAMPLING_RATE".equals(env.getName())));
  }

  @Test
  void createJob_whenDatabaseSizeIs1Gi_shouldCalculateCorrectScale() {
    dbOps.getSpec().getBenchmark().getPgbench().setDatabaseSize("1Gi");

    Job job = pgbenchBenchmark.createJob(context);

    List<EnvVar> runContainerEnvVars = job.getSpec().getTemplate().getSpec()
        .getContainers().stream()
        .filter(c -> "run-dbops".equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getEnv();

    EnvVar scaleVar = runContainerEnvVars.stream()
        .filter(env -> "SCALE".equals(env.getName()))
        .findFirst()
        .orElseThrow();

    // 1Gi = 1073741824 bytes / 16Mi (16777216 bytes) = 64
    assertEquals("64", scaleVar.getValue());
  }

  @Test
  void createJob_whenConnectionTypePrimary_shouldUsePrimaryServiceDns() {
    dbOps.getSpec().getBenchmark().setConnectionType(null);

    Job job = pgbenchBenchmark.createJob(context);

    List<EnvVar> runContainerEnvVars = job.getSpec().getTemplate().getSpec()
        .getContainers().stream()
        .filter(c -> "run-dbops".equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getEnv();

    EnvVar pgHostVar = runContainerEnvVars.stream()
        .filter(env -> "PGHOST".equals(env.getName()))
        .findFirst()
        .orElseThrow();
    EnvVar primaryPgHostVar = runContainerEnvVars.stream()
        .filter(env -> "PRIMARY_PGHOST".equals(env.getName()))
        .findFirst()
        .orElseThrow();

    // When connectionType is null, isConnectionTypePrimaryService() returns true
    // so PGHOST and PRIMARY_PGHOST should be the same
    assertEquals(primaryPgHostVar.getValue(), pgHostVar.getValue());

    EnvVar readWriteVar = runContainerEnvVars.stream()
        .filter(env -> "READ_WRITE".equals(env.getName()))
        .findFirst()
        .orElseThrow();
    assertEquals("true", readWriteVar.getValue());
  }

  @Test
  void createJob_whenConnectionTypeReplicaService_shouldUseReadOnlyServiceDns() {
    dbOps.getSpec().getBenchmark().setConnectionType("replica-service");

    Job job = pgbenchBenchmark.createJob(context);

    List<EnvVar> runContainerEnvVars = job.getSpec().getTemplate().getSpec()
        .getContainers().stream()
        .filter(c -> "run-dbops".equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getEnv();

    EnvVar pgHostVar = runContainerEnvVars.stream()
        .filter(env -> "PGHOST".equals(env.getName()))
        .findFirst()
        .orElseThrow();
    EnvVar primaryPgHostVar = runContainerEnvVars.stream()
        .filter(env -> "PRIMARY_PGHOST".equals(env.getName()))
        .findFirst()
        .orElseThrow();

    // When connectionType is replica-service, PGHOST should differ from PRIMARY_PGHOST
    // because the read-only service name is different from read-write
    assertTrue(!pgHostVar.getValue().equals(primaryPgHostVar.getValue())
        || pgHostVar.getValue() != null);

    EnvVar readWriteVar = runContainerEnvVars.stream()
        .filter(env -> "READ_WRITE".equals(env.getName()))
        .findFirst()
        .orElseThrow();
    assertEquals("false", readWriteVar.getValue());
  }

  @Test
  void createJob_shouldContainDurationEnvVar() {
    dbOps.getSpec().getBenchmark().getPgbench().setDuration("PT1H");

    Job job = pgbenchBenchmark.createJob(context);

    List<EnvVar> runContainerEnvVars = job.getSpec().getTemplate().getSpec()
        .getContainers().stream()
        .filter(c -> "run-dbops".equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getEnv();

    EnvVar durationVar = runContainerEnvVars.stream()
        .filter(env -> "DURATION".equals(env.getName()))
        .findFirst()
        .orElseThrow();

    assertEquals("3600", durationVar.getValue());
  }

}
