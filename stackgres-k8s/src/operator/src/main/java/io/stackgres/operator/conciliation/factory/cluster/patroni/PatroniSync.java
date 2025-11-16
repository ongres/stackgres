/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class PatroniSync implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster labelFactory;

  @Inject
  public PatroniSync(LabelFactoryForCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  private static String name(StackGresCluster cluster) {
    return PatroniUtil.readWriteName(cluster) + "-sync";
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    StackGresCluster cluster = context.getSource();
    return Stream.of(
        new EndpointsBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(name(cluster))
            .addToLabels(
                Optional.ofNullable(cluster.getSpec().getMetadata())
                .map(StackGresClusterSpecMetadata::getLabels)
                .map(StackGresClusterSpecLabels::getServices)
                .orElse(Map.of()))
            .addToAnnotations(
                Optional.ofNullable(cluster.getSpec().getMetadata())
                .map(StackGresClusterSpecMetadata::getAnnotations)
                .map(StackGresClusterSpecAnnotations::getServices)
                .orElse(Map.of()))
            .addToLabels(labelFactory.clusterLabels(context.getSource()))
            .endMetadata()
            .build()
    );
  }

}
