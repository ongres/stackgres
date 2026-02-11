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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsSamplingMode;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSampling;
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
class SamplingBenchmarkTest {

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

  private SamplingBenchmark samplingBenchmark;

  private StackGresDbOps dbOps;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    ObjectMapper jsonMapper = new ObjectMapper();
    samplingBenchmark = new SamplingBenchmark(
        podSecurityFactory,
        clusterStatefulSetEnvironmentVariables,
        labelFactory,
        dbOpsLabelFactory,
        jsonMapper,
        kubectl,
        dbOpsVolumeMounts,
        dbOpsTemplatesVolumeFactory);

    dbOps = Fixtures.dbOps().loadSampling().get();
    cluster = Fixtures.cluster().loadDefault().get();

    // The sampling fixture has benchmark.sampling but needs topQueriesCollectDuration
    StackGresDbOpsSampling sampling = dbOps.getSpec().getBenchmark().getSampling();
    if (sampling.getTopQueriesCollectDuration() == null) {
      sampling.setTopQueriesCollectDuration("PT10M");
    }
    if (sampling.getSamplingDuration() == null) {
      sampling.setSamplingDuration("PT1H");
    }

    lenient().when(context.getSource()).thenReturn(dbOps);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(dbOpsLabelFactory.dbOpsPodLabels(dbOps)).thenReturn(Map.of());
    lenient().when(kubectl.getImageName(cluster)).thenReturn("kubectl:latest");
  }

  @Test
  void createJob_whenSamplingOp_shouldCreateJobWithSamplingEnvVars() {
    Job job = samplingBenchmark.createJob(context);

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
        .anyMatch(env -> "TOP_QUERIES_COLLECT_DURATION".equals(env.getName())));
    assertTrue(runContainerEnvVars.stream()
        .anyMatch(env -> "SAMPLING_DURATION".equals(env.getName())));
    assertTrue(runContainerEnvVars.stream()
        .anyMatch(env -> "MODE".equals(env.getName())));
    assertTrue(runContainerEnvVars.stream()
        .anyMatch(env -> "TARGET_DATABASE".equals(env.getName())));
  }

  @Test
  void createJob_whenModeNotSet_shouldDefaultToTime() {
    dbOps.getSpec().getBenchmark().getSampling().setMode(null);

    Job job = samplingBenchmark.createJob(context);

    List<EnvVar> runContainerEnvVars = job.getSpec().getTemplate().getSpec()
        .getContainers().stream()
        .filter(c -> "run-dbops".equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getEnv();

    EnvVar modeVar = runContainerEnvVars.stream()
        .filter(env -> "MODE".equals(env.getName()))
        .findFirst()
        .orElseThrow();

    assertEquals(DbOpsSamplingMode.TIME.toString(), modeVar.getValue());
  }

  @Test
  void createJob_whenModeSetToCustom_shouldUseCustomMode() {
    dbOps.getSpec().getBenchmark().getSampling().setMode("custom");

    Job job = samplingBenchmark.createJob(context);

    List<EnvVar> runContainerEnvVars = job.getSpec().getTemplate().getSpec()
        .getContainers().stream()
        .filter(c -> "run-dbops".equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getEnv();

    EnvVar modeVar = runContainerEnvVars.stream()
        .filter(env -> "MODE".equals(env.getName()))
        .findFirst()
        .orElseThrow();

    assertEquals("custom", modeVar.getValue());
  }

  @Test
  void createJob_shouldParseDurationsCorrectly() {
    dbOps.getSpec().getBenchmark().getSampling().setTopQueriesCollectDuration("PT10M");
    dbOps.getSpec().getBenchmark().getSampling().setSamplingDuration("PT1H");

    Job job = samplingBenchmark.createJob(context);

    List<EnvVar> runContainerEnvVars = job.getSpec().getTemplate().getSpec()
        .getContainers().stream()
        .filter(c -> "run-dbops".equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getEnv();

    EnvVar topQueriesCollectDuration = runContainerEnvVars.stream()
        .filter(env -> "TOP_QUERIES_COLLECT_DURATION".equals(env.getName()))
        .findFirst()
        .orElseThrow();
    assertEquals("600", topQueriesCollectDuration.getValue());

    EnvVar samplingDuration = runContainerEnvVars.stream()
        .filter(env -> "SAMPLING_DURATION".equals(env.getName()))
        .findFirst()
        .orElseThrow();
    assertEquals("3600", samplingDuration.getValue());
  }

  @Test
  void createJob_shouldContainReadWriteEnvVar() {
    dbOps.getSpec().getBenchmark().setConnectionType(null);

    Job job = samplingBenchmark.createJob(context);

    List<EnvVar> runContainerEnvVars = job.getSpec().getTemplate().getSpec()
        .getContainers().stream()
        .filter(c -> "run-dbops".equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getEnv();

    EnvVar readWriteVar = runContainerEnvVars.stream()
        .filter(env -> "READ_WRITE".equals(env.getName()))
        .findFirst()
        .orElseThrow();

    // When connectionType is null, isConnectionTypePrimaryService() returns true
    assertEquals("true", readWriteVar.getValue());
  }

  @Test
  void createJob_shouldContainTargetDatabaseEnvVar() {
    dbOps.getSpec().getBenchmark().getSampling().setTargetDatabase("mydb");

    Job job = samplingBenchmark.createJob(context);

    List<EnvVar> runContainerEnvVars = job.getSpec().getTemplate().getSpec()
        .getContainers().stream()
        .filter(c -> "run-dbops".equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getEnv();

    EnvVar targetDbVar = runContainerEnvVars.stream()
        .filter(env -> "TARGET_DATABASE".equals(env.getName()))
        .findFirst()
        .orElseThrow();

    assertEquals("mydb", targetDbVar.getValue());
  }

}
