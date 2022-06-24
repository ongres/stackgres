/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import static io.stackgres.common.ManagedSqlUtil.generateScriptEntryHash;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScriptStatusManagerTest {

  private StackGresScript expectedScript;
  private List<StackGresScriptEntry> expectedScripts;
  private StackGresScript script;
  private List<StackGresScriptEntry> scripts;

  private final Secret secret = new SecretBuilder()
      .withData(Map.of("test", ResourceUtil.encodeSecret("CREATE USER test;")))
      .build();

  private final ConfigMap configMap = new ConfigMapBuilder()
      .withData(Map.of("test", "CREATE TABLE test();"))
      .build();

  @Mock
  ResourceFinder<ConfigMap> configMapFinder;

  @Mock
  ResourceFinder<Secret> secretFinder;

  private ScriptStatusManager statusManager;

  @BeforeEach
  void setUp() {
    statusManager = new ScriptStatusManager(configMapFinder, secretFinder);
    expectedScript = JsonUtil
        .readFromJson("stackgres_script/default.json", StackGresScript.class);
    expectedScripts = expectedScript.getSpec().getScripts();
    script = JsonUtil
        .readFromJson("stackgres_script/default.json", StackGresScript.class);
    scripts = expectedScript.getSpec().getScripts();
  }

  @Test
  void scriptWithoutHashes_shouldOnlySetHashes() {
    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(secret));
    when(configMapFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(configMap));
    expectedScript.getStatus().getScripts().get(0).setHash(
        generateScriptEntryHash(expectedScripts.get(0), "CREATE DATABASE IF NOT EXISTS test;"));
    expectedScript.getStatus().getScripts().get(1).setHash(
        generateScriptEntryHash(expectedScripts.get(1), "CREATE USER test;"));
    expectedScript.getStatus().getScripts().get(2).setHash(
        generateScriptEntryHash(expectedScripts.get(2), "CREATE TABLE test();"));

    statusManager.refreshCondition(script);

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedScript),
        JsonUtil.toJson(script));
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void scriptWithIdenticalHashes_shouldDoNothing() {
    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(secret));
    when(configMapFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(configMap));
    script.getStatus().getScripts().get(0).setHash(
        generateScriptEntryHash(scripts.get(0), "CREATE DATABASE IF NOT EXISTS test;"));
    script.getStatus().getScripts().get(1).setHash(
        generateScriptEntryHash(scripts.get(1), "CREATE USER test;"));
    script.getStatus().getScripts().get(2).setHash(
        generateScriptEntryHash(scripts.get(2), "CREATE TABLE test();"));
    expectedScript.getStatus().getScripts().get(0).setHash(
        generateScriptEntryHash(expectedScripts.get(0), "CREATE DATABASE IF NOT EXISTS test;"));
    expectedScript.getStatus().getScripts().get(1).setHash(
        generateScriptEntryHash(expectedScripts.get(1), "CREATE USER test;"));
    expectedScript.getStatus().getScripts().get(2).setHash(
        generateScriptEntryHash(expectedScripts.get(2), "CREATE TABLE test();"));

    statusManager.refreshCondition(script);

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedScript),
        JsonUtil.toJson(script));
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void scriptWithChangedHashes_shouldUpdateVersionAndSetHashes() {
    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.of("test", ResourceUtil.encodeSecret("CREATE USER test2;")))
            .build()));
    when(configMapFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(new ConfigMapBuilder()
            .withData(Map.of("test", "CREATE TABLE test2();"))
            .build()));
    script.getSpec().getScripts().get(0).setScript("CREATE DATABASE test2;");
    script.getStatus().getScripts().get(0).setHash(
        generateScriptEntryHash(scripts.get(0), "CREATE DATABASE IF NOT EXISTS test;"));
    script.getStatus().getScripts().get(1).setHash(
        generateScriptEntryHash(scripts.get(1), "CREATE USER test;"));
    script.getStatus().getScripts().get(2).setHash(
        generateScriptEntryHash(scripts.get(2), "CREATE TABLE test();"));
    expectedScript.getSpec().getScripts().get(0).setScript("CREATE DATABASE test2;");
    expectedScript.getSpec().getScripts().get(0).setVersion(1);
    expectedScript.getSpec().getScripts().get(1).setVersion(1);
    expectedScript.getSpec().getScripts().get(2).setVersion(1);
    expectedScript.getStatus().getScripts().get(0).setHash(
        generateScriptEntryHash(expectedScripts.get(0), "CREATE DATABASE test2;"));
    expectedScript.getStatus().getScripts().get(1).setHash(
        generateScriptEntryHash(expectedScripts.get(1), "CREATE USER test2;"));
    expectedScript.getStatus().getScripts().get(2).setHash(
        generateScriptEntryHash(expectedScripts.get(2), "CREATE TABLE test2();"));

    statusManager.refreshCondition(script);

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedScript),
        JsonUtil.toJson(script));
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(configMapFinder, times(1)).findByNameAndNamespace(any(), any());
  }

}
