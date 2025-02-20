/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileBuilder;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ClusterDefaultInstanceProfile implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster labelFactory;

  @Inject
  public ClusterDefaultInstanceProfile(LabelFactoryForCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    return Stream
        .of(true)
        .filter(ignored -> context.getProfile().isEmpty()
            || context.getProfile()
            .filter(instanceProfile -> labelFactory.defaultConfigLabels(context.getSource())
                .entrySet()
                .stream()
                .allMatch(label -> Optional
                    .ofNullable(instanceProfile.getMetadata().getLabels())
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .anyMatch(label::equals)))
            .map(instanceProfile -> instanceProfile.getMetadata().getOwnerReferences())
            .stream()
            .flatMap(List::stream)
            .anyMatch(ResourceUtil.getControllerOwnerReference(context.getSource())::equals))
        .map(ignored -> getDefaultProfile(context.getSource()));
  }

  private StackGresProfile getDefaultProfile(StackGresCluster cluster) {
    return new StackGresProfileBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getSpec().getSgInstanceProfile())
        .withLabels(labelFactory.defaultConfigLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .endSpec()
        .build();
  }

}
