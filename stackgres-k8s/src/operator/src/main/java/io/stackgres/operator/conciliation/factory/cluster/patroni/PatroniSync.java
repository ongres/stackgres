/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;

@Singleton
@OperatorVersionBinder
public class PatroniSync implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public PatroniSync(LabelFactoryForCluster<StackGresCluster> labelFactory) {
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
            .addToLabels(context.servicesCustomLabels())
            .addToLabels(labelFactory.clusterLabels(context.getSource()))
            .endMetadata()
            .build()
    );
  }

}
