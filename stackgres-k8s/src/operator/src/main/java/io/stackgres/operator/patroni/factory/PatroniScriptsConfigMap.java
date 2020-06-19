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
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniScriptsConfigMap implements StackGresClusterResourceStreamFactory {

  public static final String SCRIPT_NAME = "%05d-%s";

  public static final String SCRIPT_NAME_FOR_DATABASE = "%05d-%s.%s";

  private LabelFactoryDelegator factoryDelegator;

  public static String name(StackGresClusterContext clusterContext,
                            Long index, String name, String database) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + "-" + baseName(index, name, database)
        .toLowerCase(Locale.US).replaceAll("[^a-z0-9-]", "-"));
  }

  public static String scriptName(Long index, String name, String database) {
    return baseName(index, name, database) + ".sql";
  }

  private static String baseName(Long index, String name, String database) {
    name = name.replace('.', '_');
    if (database == null) {
      return String.format(SCRIPT_NAME, index, name);
    }
    return String.format(SCRIPT_NAME_FOR_DATABASE, index, name, database);
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
        .map(t -> {
          final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
          return new ConfigMapBuilder()
              .withNewMetadata()
              .withNamespace(cluster.getMetadata().getNamespace())
              .withName(name(clusterContext, t.v2, t.v1.getName(), t.v1.getDatabase()))
              .withLabels(labelFactory.patroniClusterLabels(cluster))
              .withOwnerReferences(clusterContext.getOwnerReferences())
              .endMetadata()
              .withData(ImmutableMap.of(
                  scriptName(t.v2, t.v1.getName(), t.v1.getDatabase()), t.v1.getScript()))
              .build();
        });
  }

  @Inject
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}
