/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.autoscaling;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.crd.external.autoscaling.VerticalPodAutoscalerBuilder;
import io.stackgres.common.crd.external.autoscaling.VerticalPodAutoscalerRecommenderBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterAutoscaling;
import io.stackgres.common.crd.sgcluster.StackGresClusterAutoscalingVertical;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class VerticalAutoscaling implements ResourceGenerator<StackGresClusterContext> {

  public static String name(StackGresCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName());
  }

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public VerticalAutoscaling(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    StackGresCluster cluster = context.getCluster();
    return Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getAutoscaling)
        .filter(StackGresClusterAutoscaling::isVerticalPodAutoscalingEnabled)
        .stream()
        .flatMap(autoscaling -> Stream.of(
            new VerticalPodAutoscalerBuilder()
            .withNewMetadata()
            .withLabels(labelFactory.genericLabels(cluster))
            .withName(name(cluster))
            .withNamespace(cluster.getMetadata().getNamespace())
            .endMetadata()
            .withNewSpec()
            .withNewTargetRef()
            .withApiVersion(HasMetadata.getApiVersion(StackGresCluster.class))
            .withKind(HasMetadata.getKind(StackGresCluster.class))
            .withName(cluster.getMetadata().getName())
            .endTargetRef()
            .withNewUpdatePolicy()
            .withMinReplicas(1)
            .withUpdateMode("Auto")
            .endUpdatePolicy()
            .withNewResourcePolicy()
            .addNewContainerPolicy()
            .withContainerName("*")
            .withControlledResources(List.of())
            .withMode("Off")
            .endContainerPolicy()
            .addNewContainerPolicy()
            .withContainerName(StackGresContainer.PATRONI.getName())
            .withControlledResources(Seq.<String>of()
                .append(Stream.of("cpu").filter(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                    .map(StackGresClusterAutoscaling::getMinAllowedForPatroniCpu).isPresent()))
                .append(Stream.of("memory").filter(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                    .map(StackGresClusterAutoscaling::getMinAllowedForPatroniMemory).isPresent()))
                .toList())
            .withMinAllowed(Seq.<Map.Entry<String, String>>of()
                .append(Stream.of("cpu")
                    .flatMap(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                        .map(StackGresClusterAutoscaling::getMinAllowedForPatroniCpu)
                        .map(value -> Map.entry(resource, value))
                        .stream()))
                .append(Stream.of("memory")
                    .flatMap(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                        .map(StackGresClusterAutoscaling::getMinAllowedForPatroniMemory)
                        .map(value -> Map.entry(resource, value))
                        .stream()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .withMode("Auto")
            .endContainerPolicy()
            .addNewContainerPolicy()
            .withContainerName(StackGresContainer.PGBOUNCER.getName())
            .withControlledResources(Seq.<String>of()
                .append(Stream.of("cpu").filter(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                    .map(StackGresClusterAutoscaling::getMinAllowedForPgbouncerCpu).isPresent()))
                .append(Stream.of("memory").filter(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                    .map(StackGresClusterAutoscaling::getMinAllowedForPgbouncerMemory).isPresent()))
                .toList())
            .withMinAllowed(Seq.<Map.Entry<String, String>>of()
                .append(Stream.of("cpu")
                    .flatMap(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                        .map(StackGresClusterAutoscaling::getMinAllowedForPgbouncerCpu)
                        .map(value -> Map.entry(resource, value))
                        .stream()))
                .append(Stream.of("memory")
                    .flatMap(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                        .map(StackGresClusterAutoscaling::getMinAllowedForPgbouncerMemory)
                        .map(value -> Map.entry(resource, value))
                        .stream()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .withMode("Auto")
            .endContainerPolicy()
            .addNewContainerPolicy()
            .withContainerName(StackGresContainer.ENVOY.getName())
            .withControlledResources(Seq.<String>of()
                .append(Stream.of("cpu").filter(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                    .map(StackGresClusterAutoscaling::getMinAllowedForEnvoyCpu).isPresent()))
                .append(Stream.of("memory").filter(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                    .map(StackGresClusterAutoscaling::getMinAllowedForEnvoyMemory).isPresent()))
                .toList())
            .withMinAllowed(Seq.<Map.Entry<String, String>>of()
                .append(Stream.of("cpu")
                    .flatMap(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                        .map(StackGresClusterAutoscaling::getMinAllowedForEnvoyCpu)
                        .map(value -> Map.entry(resource, value))
                        .stream()))
                .append(Stream.of("memory")
                    .flatMap(resource -> Optional.of(cluster.getSpec().getAutoscaling())
                        .map(StackGresClusterAutoscaling::getMinAllowedForEnvoyMemory)
                        .map(value -> Map.entry(resource, value))
                        .stream()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .withMode("Auto")
            .endContainerPolicy()
            .endResourcePolicy()
            .withRecommenders(Optional.ofNullable(autoscaling.getVertical())
                .map(StackGresClusterAutoscalingVertical::getRecommender)
                .map(recommender -> new VerticalPodAutoscalerRecommenderBuilder()
                    .withName(recommender)
                    .build())
                .map(List::of)
                .orElse(null))
            .endSpec()
            .build()));
  }

}
