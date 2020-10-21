/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.Locale;
import java.util.stream.Stream;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple4;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class PatroniScriptsConfigMap implements
    ResourceGenerator<StackGresClusterContext> {

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

  private static String normalizedResourceName(
      Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return (INTERNAL_SCRIPT.equals(indexedScript.v3)
        ? "internal-" + baseName(indexedScript.v1, indexedScript.v2)
        : baseName(indexedScript.v1, indexedScript.v4))
        .toLowerCase(Locale.US).replaceAll("[^a-z0-9-]", "-");
  }

  private static String normalizedKeyName(
      Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return baseName(indexedScript.v1, indexedScript.v4)
        .toLowerCase(Locale.US).replaceAll("[^a-zA-Z0-9-_.]", "-");
  }

  private static String baseName(StackGresClusterScriptEntry script, Long index) {
    if (script.getName() == null) {
      if (script.getDatabase() == null) {
        return String.format(SCRIPT_BASIC_NAME, index);
      }
      return String.format(SCRIPT_BASIC_NAME_FOR_DATABASE,
          index, script.getDatabase());
    }
    if (script.getDatabase() == null) {
      return String.format(SCRIPT_NAME, index, script.getName());
    }
    return String.format(SCRIPT_NAME_FOR_DATABASE,
        index, script.getName(), script.getDatabase());
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    return context.getIndexedScripts().stream()
        .filter(t -> t.v1.getScript() != null)
        .map(t -> new ConfigMapBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(name(context, t))
            .withLabels(context.getPatroniClusterLabels())
            .endMetadata()
            .withData(ImmutableMap.of(scriptName(t), t.v1.getScript()))
            .build());
  }
}
