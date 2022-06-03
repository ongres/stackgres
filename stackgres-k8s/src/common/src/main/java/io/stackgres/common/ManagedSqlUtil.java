/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Locale;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple3;

public interface ManagedSqlUtil {

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
        scriptEntry.getDatabase(),
        scriptEntry.getUser(),
        script);
  }

  static String defaultName(StackGresCluster cluster) {
    return cluster.getMetadata().getName() + "-default";
  }

  static String initialDataName(StackGresCluster object) {
    return object.getMetadata().getName() + "-inital-data";
  }

}
