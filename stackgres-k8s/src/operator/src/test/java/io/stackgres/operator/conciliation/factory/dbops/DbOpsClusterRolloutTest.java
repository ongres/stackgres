/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsClusterRolloutTest {

  private DbOpsClusterRollout dbOpsClusterRollout;

  private StackGresConfig config;

  private StackGresCluster cluster;

  private StackGresProfile clusterProfile;

  @Mock
  private PatroniMember patroniMember;

  @BeforeEach
  void setUp() {
    dbOpsClusterRollout = new DbOpsClusterRollout();
    config = Fixtures.config().loadDefault().get();
    cluster = Fixtures.cluster().loadDefault().get();
    clusterProfile = Fixtures.instanceProfile().loadSizeS().get();
  }

  @Test
  void generateResource_whenRestartOp_shouldGenerateClusterResource() {
    StackGresDbOps dbOps = Fixtures.dbOps().loadRestart().get();
    dbOps.getSpec().setRunAt(null);

    Pod pod = new PodBuilder()
        .withNewMetadata()
        .withName("test-0")
        .endMetadata()
        .build();

    lenient().when(patroniMember.getMember()).thenReturn("test-0");
    lenient().when(patroniMember.isPrimary()).thenReturn(true);

    StackGresDbOpsContext context = StackGresDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundCluster(cluster)
        .foundProfile(clusterProfile)
        .foundClusterPods(List.of(pod))
        .foundClusterPatroniMembers(List.of(patroniMember))
        .build();

    List<HasMetadata> resources = dbOpsClusterRollout.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertEquals(StackGresCluster.KIND, resources.get(0).getKind());
  }

  @Test
  void generateResource_whenSecurityUpgradeOp_shouldGenerateClusterResource() {
    StackGresDbOps dbOps = Fixtures.dbOps().loadSecurityUpgrade().get();
    dbOps.getSpec().setRunAt(null);

    Pod pod = new PodBuilder()
        .withNewMetadata()
        .withName("test-0")
        .endMetadata()
        .build();

    lenient().when(patroniMember.getMember()).thenReturn("test-0");
    lenient().when(patroniMember.isPrimary()).thenReturn(true);

    StackGresDbOpsContext context = StackGresDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundCluster(cluster)
        .foundProfile(clusterProfile)
        .foundClusterPods(List.of(pod))
        .foundClusterPatroniMembers(List.of(patroniMember))
        .build();

    List<HasMetadata> resources = dbOpsClusterRollout.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertEquals(StackGresCluster.KIND, resources.get(0).getKind());
  }

  @Test
  void generateResource_whenNonRolloutOp_shouldReturnEmpty() {
    StackGresDbOps dbOps = Fixtures.dbOps().loadVacuum().get();
    dbOps.getSpec().setRunAt(null);

    StackGresDbOpsContext context = StackGresDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundCluster(cluster)
        .foundProfile(clusterProfile)
        .build();

    List<HasMetadata> resources = dbOpsClusterRollout.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenScheduledInFuture_shouldReturnEmpty() {
    StackGresDbOps dbOps = Fixtures.dbOps().loadRestart().get();
    dbOps.getSpec().setRunAt(
        java.time.Instant.now().plusSeconds(3600).toString());

    StackGresDbOpsContext context = StackGresDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundCluster(cluster)
        .foundProfile(clusterProfile)
        .build();

    List<HasMetadata> resources = dbOpsClusterRollout.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenAlreadyCompleted_shouldReturnEmpty() {
    StackGresDbOps dbOps = Fixtures.dbOps().loadRestart().get();
    dbOps.getSpec().setRunAt(null);
    StackGresDbOpsStatus status = new StackGresDbOpsStatus();
    Condition completedCondition = new Condition(
        DbOpsStatusCondition.Type.COMPLETED.getType(),
        DbOpsStatusCondition.Status.TRUE.getStatus(),
        "OperationCompleted");
    status.setConditions(List.of(completedCondition));
    dbOps.setStatus(status);

    StackGresDbOpsContext context = StackGresDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundCluster(cluster)
        .foundProfile(clusterProfile)
        .build();

    List<HasMetadata> resources = dbOpsClusterRollout.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenRestartOp_clusterShouldHaveRolloutAnnotations() {
    StackGresDbOps dbOps = Fixtures.dbOps().loadRestart().get();
    dbOps.getSpec().setRunAt(null);

    Pod pod = new PodBuilder()
        .withNewMetadata()
        .withName("test-0")
        .endMetadata()
        .build();

    lenient().when(patroniMember.getMember()).thenReturn("test-0");
    lenient().when(patroniMember.isPrimary()).thenReturn(true);

    StackGresDbOpsContext context = StackGresDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundCluster(cluster)
        .foundProfile(clusterProfile)
        .foundClusterPods(List.of(pod))
        .foundClusterPatroniMembers(List.of(patroniMember))
        .build();

    List<HasMetadata> resources = dbOpsClusterRollout.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresCluster generatedCluster = (StackGresCluster) resources.get(0);
    Map<String, String> annotations = generatedCluster.getMetadata().getAnnotations();
    assertTrue(annotations.containsKey(StackGresContext.ROLLOUT_DBOPS_KEY));
    assertEquals(dbOps.getMetadata().getName(),
        annotations.get(StackGresContext.ROLLOUT_DBOPS_KEY));
  }

}
