/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple4;

@ApplicationScoped
public class PatroniScriptsConfigMap implements StackGresClusterResourceStreamFactory {

  public static final String INTERNAL_SCRIPT = "INTERNAL_SCRIPT";
  public static final String SCRIPT = "SCRIPT";
  public static final String SCRIPT_BASIC_NAME = "%05d";
  public static final String SCRIPT_BASIC_NAME_FOR_DATABASE = "%05d.%s";
  public static final String SCRIPT_NAME = "%05d-%s";
  public static final String SCRIPT_NAME_FOR_DATABASE = "%05d-%s.%s";

  private LabelFactoryDelegator factoryDelegator;

  public static String name(StackGresClusterContext clusterContext,
                            Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return ResourceUtil.cutVolumeName(
        ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
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

  @Override
  public Stream<HasMetadata> streamResources(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    return context.getIndexedScripts()
        .filter(t -> t.v1.getScript() != null)
        .map(t -> {
          final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(context);
          return new ConfigMapBuilder()
              .withNewMetadata()
              .withNamespace(cluster.getMetadata().getNamespace())
              .withName(name(context, t))
              .withLabels(labelFactory.patroniClusterLabels(cluster))
              .withOwnerReferences(context.getOwnerReferences())
              .endMetadata()
              .withData(ImmutableMap.of(scriptName(t), t.v1.getScript()))
              .build();
        });
  }

  @Inject
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}
