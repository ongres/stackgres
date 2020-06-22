/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class PatroniScriptsConfigMap implements StackGresClusterResourceStreamFactory {

  public static final String SCRIPT_BASIC_NAME = "%05d";

  public static final String SCRIPT_BASIC_NAME_FOR_DATABASE = "%05d.%s";

  public static final String SCRIPT_NAME = "%05d-%s";

  public static final String SCRIPT_NAME_FOR_DATABASE = "%05d-%s.%s";

  private LabelFactoryDelegator factoryDelegator;

  public static String name(StackGresClusterContext clusterContext,
                            Tuple2<StackGresClusterScriptEntry, Long> indexedScript) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + "-" + normalizedResourceName(indexedScript));
  }

  public static String scriptName(Tuple2<StackGresClusterScriptEntry, Long> indexedScript) {
    return normalizedKeyName(indexedScript) + ".sql";
  }

  private static String normalizedResourceName(
      Tuple2<StackGresClusterScriptEntry, Long> indexedScript) {
    return baseName(indexedScript)
        .toLowerCase(Locale.US).replaceAll("[^a-z0-9-]", "-");
  }

  private static String normalizedKeyName(
      Tuple2<StackGresClusterScriptEntry, Long> indexedScript) {
    return baseName(indexedScript)
        .toLowerCase(Locale.US).replaceAll("[^a-zA-Z0-9-_.]", "-");
  }

  private static String baseName(Tuple2<StackGresClusterScriptEntry, Long> indexedScript) {
    if (indexedScript.v1.getName() == null) {
      if (indexedScript.v1.getDatabase() == null) {
        return String.format(SCRIPT_BASIC_NAME, indexedScript.v2);
      }
      return String.format(SCRIPT_BASIC_NAME_FOR_DATABASE,
          indexedScript.v2, indexedScript.v1.getDatabase());
    }
    if (indexedScript.v1.getDatabase() == null) {
      return String.format(SCRIPT_NAME, indexedScript.v2, indexedScript.v1.getName());
    }
    return String.format(SCRIPT_NAME_FOR_DATABASE,
        indexedScript.v2, indexedScript.v1.getName(), indexedScript.v1.getDatabase());
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_LOG_LEVEL", "DEBUG");

    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getCluster();
    return Seq.of(Optional.ofNullable(
        cluster.getSpec().getInitData())
        .map(StackGresClusterInitData::getScripts))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(List::stream)
        .zipWithIndex()
        .filter(t -> t.v1.getScript() != null)
        .map(t -> {
          final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
          return new ConfigMapBuilder()
              .withNewMetadata()
              .withNamespace(cluster.getMetadata().getNamespace())
              .withName(name(clusterContext, t))
              .withLabels(labelFactory.patroniClusterLabels(cluster))
              .withOwnerReferences(clusterContext.getOwnerReferences())
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
