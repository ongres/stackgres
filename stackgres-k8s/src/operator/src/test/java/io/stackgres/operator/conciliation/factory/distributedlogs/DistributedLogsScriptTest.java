/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.DistributedLogsLabelFactory;
import io.stackgres.common.labels.DistributedLogsLabelMapper;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsScriptTest {

  private final LabelFactoryForDistributedLogs labelFactory =
      new DistributedLogsLabelFactory(new DistributedLogsLabelMapper());

  @Mock
  private StackGresDistributedLogsContext context;

  private DistributedLogsScript distributedLogsScript;

  private StackGresDistributedLogs distributedLogs;

  @BeforeEach
  void setUp() {
    distributedLogsScript = new DistributedLogsScript(labelFactory);
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    when(context.getSource()).thenReturn(distributedLogs);
    lenient().when(context.getCluster()).thenReturn(Optional.empty());
  }

  @Test
  void generateResource_shouldGenerateScriptWithInitSql() {
    when(context.getConnectedClusters()).thenReturn(List.of());

    List<HasMetadata> resources =
        distributedLogsScript.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.getFirst() instanceof StackGresScript);
    StackGresScript script = (StackGresScript) resources.getFirst();
    assertEquals(DistributedLogsScript.scriptName(distributedLogs),
        script.getMetadata().getName());
    assertEquals("distributed-logs", script.getMetadata().getNamespace());
    assertNotNull(script.getSpec().getScripts());
    assertFalse(script.getSpec().getScripts().isEmpty(),
        "Expected script entries to be non-empty");

    var installExtensions = script.getSpec().getScripts().stream()
        .filter(s -> "install-extensions".equals(s.getName()))
        .findFirst();
    assertTrue(installExtensions.isPresent(),
        "Expected an 'install-extensions' script entry");
    assertNotNull(installExtensions.get().getScript(),
        "Expected 'install-extensions' script to have SQL content");

    var init = script.getSpec().getScripts().stream()
        .filter(s -> "init".equals(s.getName()))
        .findFirst();
    assertTrue(init.isPresent(),
        "Expected an 'init' script entry");
  }

  @Test
  void generateResource_shouldReferenceConnectedClusters() {
    StackGresClusterDistributedLogs clusterDistributedLogs =
        new StackGresClusterDistributedLogs();
    clusterDistributedLogs.setSgDistributedLogs("distributedlogs");
    clusterDistributedLogs.setRetention("7 days");

    StackGresCluster connectedCluster = new StackGresClusterBuilder()
        .withNewMetadata()
        .withName("my-cluster")
        .withNamespace("my-namespace")
        .endMetadata()
        .withNewSpec()
        .withDistributedLogs(clusterDistributedLogs)
        .endSpec()
        .build();
    when(context.getConnectedClusters()).thenReturn(List.of(connectedCluster));

    List<HasMetadata> resources =
        distributedLogsScript.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresScript script = (StackGresScript) resources.getFirst();

    String expectedDbName = FluentdUtil.databaseName("my-namespace", "my-cluster");

    var updateDatabases = script.getSpec().getScripts().stream()
        .filter(s -> "update-databases".equals(s.getName()))
        .findFirst();
    assertTrue(updateDatabases.isPresent(),
        "Expected an 'update-databases' script entry");
    assertTrue(updateDatabases.get().getScript().contains(expectedDbName),
        "Expected 'update-databases' SQL to reference connected cluster database "
        + expectedDbName);

    var reconcileRetention = script.getSpec().getScripts().stream()
        .filter(s -> "reconcile-retention".equals(s.getName()))
        .findFirst();
    assertTrue(reconcileRetention.isPresent(),
        "Expected a 'reconcile-retention' script entry");
    assertTrue(reconcileRetention.get().getScript().contains(expectedDbName),
        "Expected 'reconcile-retention' SQL to reference connected cluster database "
        + expectedDbName);
  }

}
