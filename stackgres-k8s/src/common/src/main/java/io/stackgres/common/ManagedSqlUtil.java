/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryScriptStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple3;

public interface ManagedSqlUtil {

  String DEFAULT_SCRIPT_NAME_SUFFIX = "-default";
  String MANAGED_SCRIPT_PATH = "/etc/managed-script.d/";
  String SCRIPT_BASIC_NAME = "%05d-%05d";
  String SCRIPT_BASIC_NAME_FOR_DATABASE = "%05d-%05d.%s";
  String SCRIPT_NAME = "%05d-%05d-%s";
  String SCRIPT_NAME_FOR_DATABASE = "%05d-%05d-%s.%s";

  static String name(ClusterContext clusterContext,
      Tuple3<StackGresScriptEntry, Integer, Integer> indexedScript) {
    return ResourceUtil.resourceName(
        ResourceUtil.cutVolumeName(clusterContext.getCluster().getMetadata().getName()
            + "-" + normalizedResourceName(indexedScript)));
  }

  static String scriptName(
      Tuple3<StackGresScriptEntry, Integer, Integer> indexedScript) {
    return normalizedKeyName(indexedScript) + ".sql";
  }

  private static String normalizedResourceName(
      Tuple3<StackGresScriptEntry, Integer, Integer> indexedScript) {
    return "msql-" + baseName(indexedScript.v1, indexedScript.v2, indexedScript.v3)
        .toLowerCase(Locale.US).replaceAll("[^a-z0-9-]", "-");
  }

  private static String normalizedKeyName(
      Tuple3<StackGresScriptEntry, Integer, Integer> indexedScript) {
    return baseName(indexedScript.v1, indexedScript.v2, indexedScript.v3)
        .toLowerCase(Locale.US).replaceAll("[^a-zA-Z0-9-_.]", "-");
  }

  private static String baseName(StackGresScriptEntry script, Integer index,
      Integer scriptIndex) {
    if (script.getName() == null) {
      if (script.getDatabase() == null) {
        return String.format(SCRIPT_BASIC_NAME, index, scriptIndex);
      }
      return String.format(SCRIPT_BASIC_NAME_FOR_DATABASE,
          index, scriptIndex, script.getDatabase());
    }
    if (script.getDatabase() == null) {
      return String.format(SCRIPT_NAME, index, scriptIndex, script.getName());
    }
    return String.format(SCRIPT_NAME_FOR_DATABASE,
        index, scriptIndex, script.getName(), script.getDatabase());
  }

  static String generateScriptEntryHash(StackGresScriptEntry scriptEntry, String script) {
    return StackGresUtil.getMd5Sum(
        scriptEntry.getDatabaseOrDefault(),
        Optional.ofNullable(scriptEntry.getUser()).orElse("postgres"),
        scriptEntry.getWrapInTransaction(),
        String.valueOf(scriptEntry.getStoreStatusInDatabaseOrDefault()),
        String.valueOf(scriptEntry.getRetryOnErrorOrDefault()),
        script);
  }

  static String defaultName(StackGresCluster cluster) {
    return defaultName(cluster.getMetadata().getName());
  }

  static String defaultName(String clusterName) {
    return clusterName + DEFAULT_SCRIPT_NAME_SUFFIX;
  }

  static String initialDataName(StackGresCluster object) {
    return object.getMetadata().getName() + "-inital-data";
  }

  static boolean isScriptEntryUpToDate(
      StackGresScriptEntry scriptEntry,
      StackGresClusterManagedScriptEntryStatus managedScriptStatus) {
    return Optional.of(managedScriptStatus)
        .map(StackGresClusterManagedScriptEntryStatus::getScripts)
        .stream().flatMap(List::stream)
        .filter(anScriptEntryStatus -> Objects.equals(
            scriptEntry.getId(), anScriptEntryStatus.getId()))
        .anyMatch(scriptEntryStatus -> isScriptEntryUpToDate(scriptEntry, scriptEntryStatus));
  }

  static boolean isScriptEntryUpToDate(
      StackGresScriptEntry scriptEntry,
      StackGresClusterManagedScriptEntryScriptStatus mangedScriptEntryStatus) {
    return mangedScriptEntryStatus.getIntents() == null
        && Objects.equals(scriptEntry.getVersion(), mangedScriptEntryStatus.getVersion());
  }

  static boolean isScriptEntryExecutionHang(
      StackGresScriptEntry scriptEntry,
      StackGresClusterManagedScriptEntryScriptStatus mangedScriptEntryStatus) {
    return mangedScriptEntryStatus.getIntents() != null
        && mangedScriptEntryStatus.getFailureCode() == null
        && Objects.equals(scriptEntry.getVersion(), mangedScriptEntryStatus.getVersion());
  }

  static boolean isScriptEntryFailed(
      StackGresScriptEntry scriptEntry,
      StackGresClusterManagedScriptEntryScriptStatus mangedScriptEntryStatus) {
    return mangedScriptEntryStatus.getIntents() != null
        && mangedScriptEntryStatus.getFailureCode() != null
        && Objects.equals(scriptEntry.getVersion(), mangedScriptEntryStatus.getVersion());
  }

}
