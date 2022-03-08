/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterManagedSqlEventReason;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryScriptStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ManagedSqlReconciliatorTest {

  @Mock
  KubernetesClient client;
  @Mock
  ClusterControllerPropertyContext propertyContext;
  @Mock
  ResourceFinder<Endpoints> endpointsFinder;
  @Mock
  CustomResourceFinder<StackGresScript> scriptFinder;
  @Mock
  ResourceFinder<Secret> secretFinder;
  @Mock
  ResourceFinder<ConfigMap> configMapFinder;
  @Mock
  CustomResourceScheduler<StackGresCluster> clusterScheduler;
  @Mock
  EventController eventController;
  @Mock
  ManagedSqlScriptEntryExecutor managedSqlScriptEntryExecutor;
  @Mock
  StackGresClusterContext context;

  private StackGresScript script;
  private List<String> scripts;
  private Endpoints patroniEndpoints;
  private Endpoints patroniConfigEndpoints;
  private ConfigMap configMap;
  private Secret secret;

  private ManagedSqlReconciliator.Parameters parameters;
  private ManagedSqlReconciliator reconciliator;

  @BeforeEach
  void setUp() throws Exception {
    script = Fixtures.script().loadDefault().get();
    patroniEndpoints = Fixtures.endpoints().loadPatroni().get();
    patroniConfigEndpoints = Fixtures.endpoints().loadPatroniConfig().get();
    when(propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME)).thenReturn(
            patroniEndpoints.getMetadata().getAnnotations().get(PatroniUtil.LEADER_KEY));

    scripts = List.of(script.getSpec().getScripts().get(0).getScript(),
        "CREATE USER test;",
        "CREATE TABLE test();");
    secret = new SecretBuilder()
        .withData(Map.of("test", ResourceUtil.encodeSecret(scripts.get(1))))
        .build();
    script.getSpec().getScripts().get(2).setDatabase("test");
    script.getSpec().getScripts().get(2).setUser("test");
    configMap = new ConfigMapBuilder()
        .withData(Map.of("test", scripts.get(2)))
        .build();
    Seq.seq(scripts).zipWithIndex()
        .map(tuple -> Tuple.tuple(
            script.getStatus().getScripts().get(tuple.v2.intValue()),
            script.getSpec().getScripts().get(tuple.v2.intValue()),
            tuple.v1))
        .forEach(tuple -> tuple.v1.setHash(ManagedSqlUtil
            .generateScriptEntryHash(tuple.v2, tuple.v3)));

    when(propertyContext.getBoolean(
        ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_MANAGED_SQL)).thenReturn(true);

    parameters = new ManagedSqlReconciliator.Parameters();
    parameters.managedSqlScriptEntryExecutor = managedSqlScriptEntryExecutor;
    parameters.propertyContext = propertyContext;
    parameters.endpointsFinder = endpointsFinder;
    parameters.scriptFinder = scriptFinder;
    parameters.secretFinder = secretFinder;
    parameters.configMapFinder = configMapFinder;
    parameters.clusterScheduler = clusterScheduler;
    parameters.eventController = eventController;

    reconciliator = new ManagedSqlReconciliator(parameters);
  }

  @Test
  void testReconciliationWithReconciliationFlagFalse_doesNothing() throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    when(context.getCluster()).thenReturn(cluster);
    reset(propertyContext);
    when(propertyContext.getBoolean(
        ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_MANAGED_SQL)).thenReturn(false);
    reconciliator = new ManagedSqlReconciliator(parameters);

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(clusterScheduler, times(0)).update(any(), any());
    verify(eventController, times(0)).sendEvent(any(), any(), any(), any());
  }

  @Test
  void testReconciliationWithoutScripts_doesNothing() throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
    when(context.getCluster()).thenReturn(cluster);

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(clusterScheduler, times(0)).update(any(), any());
    verify(eventController, times(0)).sendEvent(any(), any(), any(), any());
  }

  @Test
  void testReconciliationWithSomeScripts_executeThemAndUpdateTheStatus() throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    final StackGresCluster originalCluster = JsonUtil.copy(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));
    when(secretFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(secret));
    when(configMapFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(configMap));
    var actualUpdatedManagedSqlStatusList = new ArrayList<StackGresClusterManagedSqlStatus>();
    when(clusterScheduler.update(any(), any())).then(invocation -> {
      addUpdatedManagedSqlStatus(invocation, cluster, actualUpdatedManagedSqlStatusList);
      return null;
    });

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(3)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(6)).update(any(), any());
    var expectedUpdatedManagedSqlStatus = originalCluster.getStatus().getManagedSql();
    var expectedUpdatedManagedSqlEntryStatus = expectedUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setScripts(new ArrayList<>());

    var actualUpdatedManagedSqlStatusIterator = actualUpdatedManagedSqlStatusList.iterator();
    var actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setStartedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setId(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus1Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setId(1);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus2Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus2Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setId(2);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus2Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus2 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setCompletedAt(
        actualUpdatedManagedSqlEntryStatus2.getCompletedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus2.getStartedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus2.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    ArgumentCaptor<ClusterManagedSqlEventReason> eventReasonArgumentCaptor =
        ArgumentCaptor.forClass(ClusterManagedSqlEventReason.class);
    verify(eventController, times(3)).sendEvent(
        eventReasonArgumentCaptor.capture(), any(), any(), any());
    assertEquals(List.of(
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL),
        eventReasonArgumentCaptor.getAllValues());
  }

  @Test
  void testReconciliationWithAlreadyRunScript_executeOthersAndUpdateTheStatus() throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    cluster.getStatus().getManagedSql().getScripts().get(0).setStartedAt(Instant.now().toString());
    cluster.getStatus().getManagedSql().getScripts().get(0).setScripts(new ArrayList<>());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0).setId(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0).setVersion(0);
    final StackGresCluster originalCluster = JsonUtil.copy(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));
    when(secretFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(secret));
    when(configMapFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(configMap));
    var actualUpdatedManagedSqlStatusList = new ArrayList<StackGresClusterManagedSqlStatus>();
    when(clusterScheduler.update(any(), any())).then(invocation -> {
      addUpdatedManagedSqlStatus(invocation, cluster, actualUpdatedManagedSqlStatusList);
      return null;
    });

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(2)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(4)).update(any(), any());
    var expectedUpdatedManagedSqlStatus = originalCluster.getStatus().getManagedSql();
    var expectedUpdatedManagedSqlEntryStatus = expectedUpdatedManagedSqlStatus.getScripts().get(0);

    var actualUpdatedManagedSqlStatusIterator = actualUpdatedManagedSqlStatusList.iterator();
    var actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setStartedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setId(1);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus1Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setId(2);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setCompletedAt(
        actualUpdatedManagedSqlEntryStatus1.getCompletedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getStartedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    ArgumentCaptor<ClusterManagedSqlEventReason> eventReasonArgumentCaptor =
        ArgumentCaptor.forClass(ClusterManagedSqlEventReason.class);
    verify(eventController, times(2)).sendEvent(
        eventReasonArgumentCaptor.capture(), any(), any(), any());
    assertEquals(List.of(
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL),
        eventReasonArgumentCaptor.getAllValues());
  }

  @Test
  void testReconciliationWithNewScriptVersion_executeItAndUpdateTheStatus() throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    cluster.getStatus().getManagedSql().getScripts().get(0).setStartedAt(Instant.now().toString());
    cluster.getStatus().getManagedSql().getScripts().get(0).setScripts(new ArrayList<>());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0).setId(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0).setVersion(0);
    final StackGresCluster originalCluster = JsonUtil.copy(cluster);
    script.getSpec().getScripts().get(0).setVersion(1);
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));
    when(secretFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(secret));
    when(configMapFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(configMap));
    var actualUpdatedManagedSqlStatusList = new ArrayList<StackGresClusterManagedSqlStatus>();
    when(clusterScheduler.update(any(), any())).then(invocation -> {
      addUpdatedManagedSqlStatus(invocation, cluster, actualUpdatedManagedSqlStatusList);
      return null;
    });

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(3)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(6)).update(any(), any());
    var expectedUpdatedManagedSqlStatus = originalCluster.getStatus().getManagedSql();
    var expectedUpdatedManagedSqlEntryStatus = expectedUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setScripts(new ArrayList<>());

    var actualUpdatedManagedSqlStatusIterator = actualUpdatedManagedSqlStatusList.iterator();
    var actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setStartedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setId(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setVersion(1);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus1Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setId(1);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus2Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus2Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setId(2);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus2Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus2 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setCompletedAt(
        actualUpdatedManagedSqlEntryStatus2.getCompletedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus2.getStartedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus2.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    ArgumentCaptor<ClusterManagedSqlEventReason> eventReasonArgumentCaptor =
        ArgumentCaptor.forClass(ClusterManagedSqlEventReason.class);
    verify(eventController, times(3)).sendEvent(
        eventReasonArgumentCaptor.capture(), any(), any(), any());
    assertEquals(List.of(
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL),
        eventReasonArgumentCaptor.getAllValues());
  }

  @Test
  void testReconciliationWithAFailingScript_executeUpToTheFailingScriptAndUpdateTheStatus()
      throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    final StackGresCluster originalCluster = JsonUtil.copy(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));
    when(secretFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(secret));
    doNothing().doThrow(new RuntimeException("test"))
        .when(managedSqlScriptEntryExecutor).executeScriptEntry(any(), any());
    var actualUpdatedManagedSqlStatusList = new ArrayList<StackGresClusterManagedSqlStatus>();
    when(clusterScheduler.update(any(), any())).then(invocation -> {
      addUpdatedManagedSqlStatus(invocation, cluster, actualUpdatedManagedSqlStatusList);
      return null;
    });

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(2)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(4)).update(any(), any());
    var expectedUpdatedManagedSqlStatus = originalCluster.getStatus().getManagedSql();
    var expectedUpdatedManagedSqlEntryStatus = expectedUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setScripts(new ArrayList<>());

    var actualUpdatedManagedSqlStatusIterator = actualUpdatedManagedSqlStatusList.iterator();
    var actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setStartedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setId(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus1Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setId(1);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setFailedAt(
        actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(1);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setFailureCode("XX500");
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setFailure("test");
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getCompletedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    ArgumentCaptor<ClusterManagedSqlEventReason> eventReasonArgumentCaptor =
        ArgumentCaptor.forClass(ClusterManagedSqlEventReason.class);
    verify(eventController, times(2)).sendEvent(
        eventReasonArgumentCaptor.capture(), any(), any(), any());
    assertEquals(List.of(
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL_ERROR),
        eventReasonArgumentCaptor.getAllValues());
  }

  @Test
  void testReconciliationWithAHashMismatch_shouldDoNothing()
      throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    script.getStatus().getScripts().get(0).setHash("wrong");
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(0)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(0)).update(any(), any());
    verify(eventController, times(0)).sendEvent(any(), any(), any(), any());
  }

  @Test
  void testReconciliationWithContinueOnFailure_executeThemAndUpdateTheStatus()
      throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    script.getSpec().setContinueOnError(true);
    script.getSpec().getScripts().get(0).setRetryOnError(true);
    final StackGresCluster originalCluster = JsonUtil.copy(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));
    when(secretFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(secret));
    when(configMapFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(configMap));
    doNothing().doThrow(new RuntimeException("test")).doNothing()
        .when(managedSqlScriptEntryExecutor).executeScriptEntry(any(), any());
    var actualUpdatedManagedSqlStatusList = new ArrayList<StackGresClusterManagedSqlStatus>();
    when(clusterScheduler.update(any(), any())).then(invocation -> {
      addUpdatedManagedSqlStatus(invocation, cluster, actualUpdatedManagedSqlStatusList);
      return null;
    });

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(3)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(6)).update(any(), any());
    var expectedUpdatedManagedSqlStatus = originalCluster.getStatus().getManagedSql();
    var expectedUpdatedManagedSqlEntryStatus = expectedUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setScripts(new ArrayList<>());

    var actualUpdatedManagedSqlStatusIterator = actualUpdatedManagedSqlStatusList.iterator();
    var actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setStartedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setId(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus1Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setId(1);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setFailedAt(
        actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(1);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setFailureCode("XX500");
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setFailure("test");
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getCompletedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus2Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus2Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setId(2);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus2Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2Intent.getCompletedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus2Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus2 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus2.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2.getCompletedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus2.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    ArgumentCaptor<ClusterManagedSqlEventReason> eventReasonArgumentCaptor =
        ArgumentCaptor.forClass(ClusterManagedSqlEventReason.class);
    verify(eventController, times(3)).sendEvent(
        eventReasonArgumentCaptor.capture(), any(), any(), any());
    assertEquals(List.of(
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL_ERROR,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL),
        eventReasonArgumentCaptor.getAllValues());
  }

  @Test
  void testReconciliationOfFailedScriptWithoutRetryOnError_skipExecutionAfterAFailure()
      throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    script.getSpec().setContinueOnError(false);
    script.getSpec().getScripts().get(0).setRetryOnError(false);
    cluster.getStatus().getManagedSql().getScripts().get(0).setScripts(new ArrayList<>());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setId(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setVersion(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setIntents(1);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setFailureCode("test");
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setFailure("test");
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(0)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(0)).update(any(), any());
    verify(eventController, times(0)).sendEvent(any(), any(), any(), any());
  }

  @Test
  void testReconciliationOfScriptWithIntentWithoutRetryOnError_skipExecutionAfterAFailure()
      throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    script.getSpec().setContinueOnError(false);
    script.getSpec().getScripts().get(0).setRetryOnError(false);
    cluster.getStatus().getManagedSql().getScripts().get(0).setScripts(new ArrayList<>());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setId(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setVersion(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setIntents(1);
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(0)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(0)).update(any(), any());
    verify(eventController, times(0)).sendEvent(any(), any(), any(), any());
  }

  @Test
  void testReconciliationOfFailedScriptWithRetryOnError_executeAfterAFailure()
      throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    script.getSpec().setContinueOnError(false);
    script.getSpec().getScripts().get(0).setRetryOnError(true);
    cluster.getStatus().getManagedSql().getScripts().get(0).setScripts(new ArrayList<>());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setId(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setVersion(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setIntents(1);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setFailureCode("test");
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setFailure("test");
    final StackGresCluster originalCluster = JsonUtil.copy(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));
    when(secretFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(secret));
    when(configMapFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(configMap));
    var actualUpdatedManagedSqlStatusList = new ArrayList<StackGresClusterManagedSqlStatus>();
    when(clusterScheduler.update(any(), any())).then(invocation -> {
      addUpdatedManagedSqlStatus(invocation, cluster, actualUpdatedManagedSqlStatusList);
      return null;
    });

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(3)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(6)).update(any(), any());
    var expectedUpdatedManagedSqlStatus = originalCluster.getStatus().getManagedSql();
    var expectedUpdatedManagedSqlEntryStatus = expectedUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setScripts(new ArrayList<>());

    var actualUpdatedManagedSqlStatusIterator = actualUpdatedManagedSqlStatusList.iterator();
    var actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setStartedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setId(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(2);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setFailureCode(null);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setFailure(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus1Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setId(1);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus2Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus2Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setId(2);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus2Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus2 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setCompletedAt(
        actualUpdatedManagedSqlEntryStatus2.getCompletedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus2.getStartedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus2.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    ArgumentCaptor<ClusterManagedSqlEventReason> eventReasonArgumentCaptor =
        ArgumentCaptor.forClass(ClusterManagedSqlEventReason.class);
    verify(eventController, times(3)).sendEvent(
        eventReasonArgumentCaptor.capture(), any(), any(), any());
    assertEquals(List.of(
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL),
        eventReasonArgumentCaptor.getAllValues());
  }

  @Test
  void testReconciliationOfReFailedScriptWithRetryOnError_retryTheFailedScriptOnly()
      throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    script.getSpec().setContinueOnError(false);
    script.getSpec().getScripts().get(0).setRetryOnError(true);
    cluster.getStatus().getManagedSql().getScripts().get(0).setScripts(new ArrayList<>());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setId(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setVersion(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setIntents(1);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setFailureCode("test");
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setFailure("test");
    final StackGresCluster originalCluster = JsonUtil.copy(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));
    doThrow(new RuntimeException("test"))
        .when(managedSqlScriptEntryExecutor).executeScriptEntry(any(), any());
    var actualUpdatedManagedSqlStatusList = new ArrayList<StackGresClusterManagedSqlStatus>();
    when(clusterScheduler.update(any(), any())).then(invocation -> {
      addUpdatedManagedSqlStatus(invocation, cluster, actualUpdatedManagedSqlStatusList);
      return null;
    });

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(1)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(2)).update(any(), any());
    var expectedUpdatedManagedSqlStatus = originalCluster.getStatus().getManagedSql();
    var expectedUpdatedManagedSqlEntryStatus = expectedUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setScripts(new ArrayList<>());

    var actualUpdatedManagedSqlStatusIterator = actualUpdatedManagedSqlStatusList.iterator();
    var actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setStartedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setId(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(2);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setFailureCode(null);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setFailure(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setFailedAt(
        actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(2);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setFailureCode("XX500");
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setFailure("test");
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getCompletedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    ArgumentCaptor<ClusterManagedSqlEventReason> eventReasonArgumentCaptor =
        ArgumentCaptor.forClass(ClusterManagedSqlEventReason.class);
    verify(eventController, times(1)).sendEvent(
        eventReasonArgumentCaptor.capture(), any(), any(), any());
    assertEquals(List.of(
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL_ERROR),
        eventReasonArgumentCaptor.getAllValues());
  }

  @Test
  void testReconciliationOfScriptWithIntentWithRetryOnError_executeAfterAFailure()
      throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    script.getSpec().setContinueOnError(false);
    script.getSpec().getScripts().get(0).setRetryOnError(true);
    cluster.getStatus().getManagedSql().getScripts().get(0).setScripts(new ArrayList<>());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setId(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setVersion(0);
    cluster.getStatus().getManagedSql().getScripts().get(0).getScripts().get(0)
        .setIntents(1);
    final StackGresCluster originalCluster = JsonUtil.copy(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));
    when(secretFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(secret));
    when(configMapFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(configMap));
    var actualUpdatedManagedSqlStatusList = new ArrayList<StackGresClusterManagedSqlStatus>();
    when(clusterScheduler.update(any(), any())).then(invocation -> {
      addUpdatedManagedSqlStatus(invocation, cluster, actualUpdatedManagedSqlStatusList);
      return null;
    });

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(3)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(6)).update(any(), any());
    var expectedUpdatedManagedSqlStatus = originalCluster.getStatus().getManagedSql();
    var expectedUpdatedManagedSqlEntryStatus = expectedUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setScripts(new ArrayList<>());

    var actualUpdatedManagedSqlStatusIterator = actualUpdatedManagedSqlStatusList.iterator();
    var actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setStartedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setId(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(2);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(0).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus1Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setId(1);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(1).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus2Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus2Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setId(2);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus2Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus2 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus.setCompletedAt(
        actualUpdatedManagedSqlEntryStatus2.getCompletedAt());
    expectedUpdatedManagedSqlEntryStatus.getScripts().get(2).setIntents(null);
    assertNotNull(actualUpdatedManagedSqlEntryStatus2.getStartedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus2.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus2.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    ArgumentCaptor<ClusterManagedSqlEventReason> eventReasonArgumentCaptor =
        ArgumentCaptor.forClass(ClusterManagedSqlEventReason.class);
    verify(eventController, times(3)).sendEvent(
        eventReasonArgumentCaptor.capture(), any(), any(), any());
    assertEquals(List.of(
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL),
        eventReasonArgumentCaptor.getAllValues());
  }

  @Test
  void testReconciliationWithContinueOnManagedScriptError_executeThemAndUpdateTheStatus()
      throws Exception {
    final StackGresCluster cluster = Fixtures.cluster().loadManagedSql().get();
    script.getSpec().setScripts(script.getSpec().getScripts().subList(0, 1));
    script.getStatus().setScripts(script.getStatus().getScripts().subList(0, 1));
    cluster.getSpec().getManagedSql().setContinueOnScriptError(true);
    cluster.getSpec().getManagedSql().getScripts().add(
        JsonUtil.copy(cluster.getSpec().getManagedSql().getScripts().get(0)));
    cluster.getSpec().getManagedSql().getScripts().get(0).setId(1);
    cluster.getStatus().getManagedSql().getScripts().add(
        JsonUtil.copy(cluster.getStatus().getManagedSql().getScripts().get(0)));
    cluster.getStatus().getManagedSql().getScripts().get(0).setId(1);
    final StackGresCluster originalCluster = JsonUtil.copy(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.name(cluster)), any()))
        .thenReturn(Optional.of(patroniEndpoints));
    when(endpointsFinder.findByNameAndNamespace(eq(PatroniUtil.configName(cluster)), any()))
        .thenReturn(Optional.of(patroniConfigEndpoints));
    when(scriptFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(script));
    doThrow(new RuntimeException("test"))
        .when(managedSqlScriptEntryExecutor).executeScriptEntry(any(), any());
    var actualUpdatedManagedSqlStatusList = new ArrayList<StackGresClusterManagedSqlStatus>();
    when(clusterScheduler.update(any(), any())).then(invocation -> {
      addUpdatedManagedSqlStatus(invocation, cluster, actualUpdatedManagedSqlStatusList);
      return null;
    });

    reconciliator.reconcile(client, context);

    verify(endpointsFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(scriptFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(managedSqlScriptEntryExecutor, times(2)).executeScriptEntry(any(), any());
    verify(clusterScheduler, times(4)).update(any(), any());
    var expectedUpdatedManagedSqlStatus = originalCluster.getStatus().getManagedSql();
    var expectedUpdatedManagedSqlEntryStatus0 = expectedUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus0.setScripts(new ArrayList<>());

    var actualUpdatedManagedSqlStatusIterator = actualUpdatedManagedSqlStatusList.iterator();
    var actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus0.setStartedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus0.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus0Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus0.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus0.getScripts().get(0).setId(0);
    expectedUpdatedManagedSqlEntryStatus0.getScripts().get(0).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus0.getScripts().get(0).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus0Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus0 =
        actualUpdatedManagedSqlStatus.getScripts().get(0);
    expectedUpdatedManagedSqlEntryStatus0.setFailedAt(
        actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    expectedUpdatedManagedSqlEntryStatus0.getScripts().get(0).setIntents(1);
    expectedUpdatedManagedSqlEntryStatus0.getScripts().get(0).setFailureCode("XX500");
    expectedUpdatedManagedSqlEntryStatus0.getScripts().get(0).setFailure("test");
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus0.getCompletedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus0.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    var expectedUpdatedManagedSqlEntryStatus1 = expectedUpdatedManagedSqlStatus.getScripts().get(1);
    expectedUpdatedManagedSqlEntryStatus1.setScripts(new ArrayList<>());

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1Intent =
        actualUpdatedManagedSqlStatus.getScripts().get(1);
    expectedUpdatedManagedSqlEntryStatus1.setUpdatedAt(
        actualUpdatedManagedSqlEntryStatus1Intent.getUpdatedAt());
    expectedUpdatedManagedSqlEntryStatus1.setStartedAt(
        actualUpdatedManagedSqlEntryStatus1Intent.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus1.getScripts()
        .add(new StackGresClusterManagedScriptEntryScriptStatus());
    expectedUpdatedManagedSqlEntryStatus1.getScripts().get(0).setId(0);
    expectedUpdatedManagedSqlEntryStatus1.getScripts().get(0).setVersion(0);
    expectedUpdatedManagedSqlEntryStatus1.getScripts().get(0).setIntents(1);
    assertNotNull(actualUpdatedManagedSqlEntryStatus1Intent.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getCompletedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1Intent.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    actualUpdatedManagedSqlStatus = actualUpdatedManagedSqlStatusIterator.next();
    var actualUpdatedManagedSqlEntryStatus1 =
        actualUpdatedManagedSqlStatus.getScripts().get(1);
    expectedUpdatedManagedSqlEntryStatus1.setStartedAt(
        actualUpdatedManagedSqlEntryStatus1.getStartedAt());
    expectedUpdatedManagedSqlEntryStatus1.setFailedAt(
        actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    expectedUpdatedManagedSqlEntryStatus1.getScripts().get(0).setIntents(1);
    expectedUpdatedManagedSqlEntryStatus1.getScripts().get(0).setFailureCode("XX500");
    expectedUpdatedManagedSqlEntryStatus1.getScripts().get(0).setFailure("test");
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getStartedAt());
    assertNull(actualUpdatedManagedSqlEntryStatus1.getCompletedAt());
    assertNotNull(actualUpdatedManagedSqlEntryStatus1.getFailedAt());
    JsonUtil.assertJsonEquals(JsonUtil.toJson(expectedUpdatedManagedSqlStatus),
        JsonUtil.toJson(actualUpdatedManagedSqlStatus));

    ArgumentCaptor<ClusterManagedSqlEventReason> eventReasonArgumentCaptor =
        ArgumentCaptor.forClass(ClusterManagedSqlEventReason.class);
    verify(eventController, times(2)).sendEvent(
        eventReasonArgumentCaptor.capture(), any(), any(), any());
    assertEquals(List.of(
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL_ERROR,
        ClusterManagedSqlEventReason.CLUSTER_MANAGED_SQL_ERROR),
        eventReasonArgumentCaptor.getAllValues());
  }

  @SuppressWarnings("unchecked")
  private void addUpdatedManagedSqlStatus(InvocationOnMock invocation, StackGresCluster cluster,
      ArrayList<StackGresClusterManagedSqlStatus> actualUpdatedManagedSqlStatusList) {
    var updater = (BiConsumer<StackGresCluster, StackGresCluster>)
        invocation.getArgument(1);
    var updatedCluster = Fixtures.cluster().loadManagedSql().get();
    updater.accept(updatedCluster, cluster);
    actualUpdatedManagedSqlStatusList.add(
        JsonUtil.copy(updatedCluster.getStatus().getManagedSql()));
  }
}
