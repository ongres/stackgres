/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class ShardedClusterServices implements
    ResourceGenerator<StackGresShardedClusterContext> {

  private LabelFactoryForShardedCluster labelFactory;

  public static String readWriteName(StackGresShardedClusterContext clusterContext) {
    return PatroniUtil.readWriteName(clusterContext.getSource().getMetadata().getName());
  }

  /**
   * Create the Services associated with the cluster.
   */
  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    Seq<HasMetadata> services = Seq.of();

    services = services.append(createPrimaryService(context));

    return services;
  }

  private Service createPrimaryService(StackGresShardedClusterContext context) {
    StackGresShardedCluster sharedCluster = context.getSource();
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(sharedCluster.getMetadata().getNamespace())
        .withName(readWriteName(context))
        .addToLabels(labelFactory.genericLabels(sharedCluster))
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
        .addAllToPorts(List.of(
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_PORT_NAME)
                .withPort(PatroniUtil.POSTGRES_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_PORT_NAME))
                .build(),
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(PatroniUtil.REPLICATION_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME))
                .build()))
        .withType("ClusterIP")
        .endSpec()
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

}
