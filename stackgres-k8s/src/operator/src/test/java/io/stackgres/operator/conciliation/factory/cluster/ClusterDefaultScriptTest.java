/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterDefaultScriptTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private ClusterDefaultScript clusterDefaultScript;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    clusterDefaultScript = new ClusterDefaultScript(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);
  }

  @Test
  void generateResource_defaultFlavor_shouldGenerateTwoScriptEntries() {
    List<HasMetadata> resources = clusterDefaultScript.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresScript script = (StackGresScript) resources.getFirst();
    List<StackGresScriptEntry> entries = script.getSpec().getScripts();
    assertEquals(2, entries.size());
    assertEquals("prometheus-postgres-exporter-init", entries.get(0).getName());
    assertEquals("password-update", entries.get(1).getName());
  }

  @Test
  void generateResource_babelfishFlavor_shouldGenerateFiveScriptEntries() {
    cluster.getSpec().getPostgres().setFlavor("babelfish");

    List<HasMetadata> resources = clusterDefaultScript.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresScript script = (StackGresScript) resources.getFirst();
    List<StackGresScriptEntry> entries = script.getSpec().getScripts();
    assertEquals(5, entries.size());
    assertEquals("prometheus-postgres-exporter-init", entries.get(0).getName());
    assertEquals("babelfish-user", entries.get(1).getName());
    assertEquals("babelfish-database", entries.get(2).getName());
    assertEquals("babelfish-init", entries.get(3).getName());
    assertEquals("password-update", entries.get(4).getName());
  }

  @Test
  void generateResource_shouldHaveCorrectName() {
    List<HasMetadata> resources = clusterDefaultScript.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresScript script = (StackGresScript) resources.getFirst();
    String expectedName = ManagedSqlUtil.defaultName(cluster);
    assertEquals(expectedName, script.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(), script.getMetadata().getNamespace());
  }

  @Test
  void generateResource_passwordUpdateReferencesPatroniSecret() {
    List<HasMetadata> resources = clusterDefaultScript.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresScript script = (StackGresScript) resources.getFirst();
    List<StackGresScriptEntry> entries = script.getSpec().getScripts();
    StackGresScriptEntry passwordUpdate = entries.stream()
        .filter(e -> "password-update".equals(e.getName()))
        .findFirst()
        .orElseThrow();
    assertNotNull(passwordUpdate.getScriptFrom());
    assertNotNull(passwordUpdate.getScriptFrom().getSecretKeyRef());
    assertEquals(PatroniSecret.name(cluster),
        passwordUpdate.getScriptFrom().getSecretKeyRef().getName());
    assertEquals(StackGresPasswordKeys.ROLES_UPDATE_SQL_KEY,
        passwordUpdate.getScriptFrom().getSecretKeyRef().getKey());
  }

}
