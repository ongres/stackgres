/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple4;

public class PatroniScriptsConfigMap {

  public static final String INTERNAL_SCRIPT = "INTERNAL_SCRIPT";
  public static final String SCRIPT = "SCRIPT";
  public static final String SCRIPT_BASIC_NAME = "%05d";
  public static final String SCRIPT_BASIC_NAME_FOR_DATABASE = "%05d.%s";
  public static final String SCRIPT_NAME = "%05d-%s";
  public static final String SCRIPT_NAME_FOR_DATABASE = "%05d-%s.%s";

  public static String name(StackGresClusterContext clusterContext,
                            Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return ResourceUtil.cutVolumeName(
        ResourceUtil.resourceName(clusterContext.getSource().getMetadata().getName()
            + "-" + normalizedResourceName(indexedScript)));
  }

  public static String scriptName(
      Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return normalizedKeyName(indexedScript) + ".sql";
  }

  public static String normalizedResourceName(
      Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return ResourceUtil.resourceName(
        ResourceUtil.sanitizedResourceName(INTERNAL_SCRIPT.equals(indexedScript.v3)
            ? "internal-" + baseName(indexedScript.v1, indexedScript.v2)
            : baseName(indexedScript.v1, indexedScript.v4)));
  }

  public static String normalizedKeyName(
      Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return baseName(indexedScript.v1, indexedScript.v4);
  }

  private static String baseName(StackGresClusterScriptEntry script, Long index) {
    if (script.getName() == null) {
      if (script.getDatabase() == null) {
        return String.format(SCRIPT_BASIC_NAME, index);
      }
      return String.format(SCRIPT_BASIC_NAME_FOR_DATABASE,
          index, encodeDatabase(script.getDatabase()));
    }
    if (script.getDatabase() == null) {
      return String.format(SCRIPT_NAME, index,
          ResourceUtil.sanitizedResourceName(script.getName()));
    }
    return String.format(SCRIPT_NAME_FOR_DATABASE,
        index,
        ResourceUtil.sanitizedResourceName(script.getName()),
        encodeDatabase(script.getDatabase()));
  }

  public static String encodeDatabase(String database) {
    return database.replaceAll("\\\\", "\\\\\\\\").replaceAll("/", "\\\\h");
  }

}
