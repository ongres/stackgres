/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniScriptsConfigMap implements StackGresClusterResourceStreamFactory {

  public static final String SCRIPT_NAME = "script-%05d";

  public static String name(StackGresClusterContext clusterContext, Long index) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + "-" + baseName(index));
  }

  public static String scriptName(Long index) {
    return baseName(index) + ".sql";
  }

  private static String baseName(Long index) {
    return String.format(SCRIPT_NAME, index);
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_LOG_LEVEL", "DEBUG");

    return Seq.of(Optional.of(context.getClusterContext().getCluster().getSpec().getInitData())
        .map(StackGresClusterInitData::getScripts))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(List::stream)
        .zipWithIndex()
        .map(t -> new ConfigMapBuilder()
            .withNewMetadata()
            .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
            .withName(name(context.getClusterContext(), t.v2))
            .withLabels(context.getClusterContext().patroniClusterLabels())
            .withOwnerReferences(context.getClusterContext().ownerReference())
            .endMetadata()
            .withData(ImmutableMap.of(scriptName(t.v2), t.v1))
            .build());
  }

}
