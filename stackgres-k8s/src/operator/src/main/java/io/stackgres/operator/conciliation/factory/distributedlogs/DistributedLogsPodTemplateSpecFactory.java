/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodAffinityTermBuilder;
import io.fabric8.kubernetes.api.model.PodAntiAffinityBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPodScheduling;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.InitContainerFactoryDiscover;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ImmutablePodTemplateResult;
import io.stackgres.operator.conciliation.factory.PodTemplateFactory;
import io.stackgres.operator.conciliation.factory.PodTemplateResult;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.distributedlogs.patroni.PatroniRole;
import org.jooq.lambda.Seq;

@Singleton
public class DistributedLogsPodTemplateSpecFactory
    implements PodTemplateFactory<DistributedLogsContainerContext> {

  private final ResourceFactory<StackGresDistributedLogsContext, PodSecurityContext>
      podSecurityContext;

  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  private final ContainerFactoryDiscoverer<DistributedLogsContainerContext>
      containerFactoryDiscoverer;

  private final InitContainerFactoryDiscover<DistributedLogsContainerContext>
      initContainerFactoryDiscoverer;

  @Inject
  public DistributedLogsPodTemplateSpecFactory(
      ResourceFactory<StackGresDistributedLogsContext, PodSecurityContext> podSecurityContext,
      LabelFactoryForCluster<StackGresDistributedLogs> labelFactory,
      ContainerFactoryDiscoverer<DistributedLogsContainerContext> containerFactoryDiscoverer,
      InitContainerFactoryDiscover<DistributedLogsContainerContext> iniContainerFactoryDiscoverer) {
    this.podSecurityContext = podSecurityContext;
    this.labelFactory = labelFactory;
    this.containerFactoryDiscoverer = containerFactoryDiscoverer;
    this.initContainerFactoryDiscoverer = iniContainerFactoryDiscoverer;
  }

  @Override
  public PodTemplateResult getPodTemplateSpec(DistributedLogsContainerContext context) {
    StackGresDistributedLogs cluster = context.getDistributedLogsContext().getSource();

    final Map<String, String> podLabels = labelFactory.statefulSetPodLabels(cluster);

    List<ContainerFactory<DistributedLogsContainerContext>> containerFactories =
        containerFactoryDiscoverer.discoverContainers(context);

    List<Container> containers = containerFactories.stream()
        .map(f -> f.getContainer(context)).collect(Collectors.toUnmodifiableList());

    final List<ContainerFactory<DistributedLogsContainerContext>> initContainerFactories
        = initContainerFactoryDiscoverer.discoverContainers(context);

    List<Container> initContainers = initContainerFactories
        .stream().map(f -> f.getContainer(context))
        .collect(Collectors.toUnmodifiableList());

    final List<String> claimedVolumes = Stream.concat(containers.stream(), initContainers.stream())
        .flatMap(container -> container.getVolumeMounts().stream())
        .map(VolumeMount::getName)
        .distinct()
        .collect(Collectors.toUnmodifiableList());

    claimedVolumes.forEach(rv -> {
      if (!context.availableVolumes().containsKey(rv) && !context.getDataVolumeName().equals(rv)) {
        throw new IllegalStateException("Volume " + rv + " is required but not available");
      }
    });

    List<Volume> volumes = claimedVolumes.stream()
        .map(volumeName -> context.availableVolumes().get(volumeName))
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableList());

    var podTemplateSpec = new PodTemplateSpecBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .addToLabels(podLabels)
            .addToAnnotations(StackGresContext.VERSION_KEY, cluster.getMetadata().getAnnotations()
                .getOrDefault(StackGresContext.VERSION_KEY,
                    StackGresProperty.OPERATOR_VERSION.getString()))
            .build())
        .withNewSpec()
        .withAffinity(Optional.of(new AffinityBuilder()
            .withPodAntiAffinity(new PodAntiAffinityBuilder()
                .addAllToRequiredDuringSchedulingIgnoredDuringExecution(ImmutableList.of(
                    new PodAffinityTermBuilder()
                        .withLabelSelector(new LabelSelectorBuilder()
                            .withMatchExpressions(new LabelSelectorRequirementBuilder()
                                    .withKey(StackGresContext.APP_KEY)
                                    .withOperator("In")
                                    .withValues(labelFactory.labelMapper().appName())
                                    .build(),
                                new LabelSelectorRequirementBuilder()
                                    .withKey("cluster")
                                    .withOperator("In")
                                    .withValues("true")
                                    .build())
                            .build())
                        .withTopologyKey("kubernetes.io/hostname")
                        .build()))
                .build())
            .build())
            .filter(affinity -> Optional.ofNullable(
                cluster.getSpec().getNonProductionOptions())
                .map(StackGresDistributedLogsNonProduction::getDisableClusterPodAntiAffinity)
                .map(disableClusterPodAntiAffinity -> !disableClusterPodAntiAffinity)
                .orElse(true))
            .orElse(null))
        .withNodeSelector(Optional.ofNullable(cluster.getSpec())
            .map(StackGresDistributedLogsSpec::getScheduling)
            .map(StackGresDistributedLogsPodScheduling::getNodeSelector)
            .orElse(null))
        .withTolerations(Optional.ofNullable(cluster.getSpec())
            .map(StackGresDistributedLogsSpec::getScheduling)
            .map(StackGresDistributedLogsPodScheduling::getTolerations)
            .map(tolerations -> Seq.seq(tolerations)
                .map(TolerationBuilder::new)
                .map(TolerationBuilder::build)
                .toList())
            .orElse(null))
        .withShareProcessNamespace(Boolean.TRUE)
        .withServiceAccountName(PatroniRole.roleName(context.getDistributedLogsContext()))
        .withSecurityContext(podSecurityContext.createResource(context.getDistributedLogsContext()))
        .withVolumes(volumes)
        .withContainers(containers)
        .withInitContainers(initContainers)
        .withTerminationGracePeriodSeconds(60L)
        .endSpec()
        .build();

    return ImmutablePodTemplateResult.builder()
        .spec(podTemplateSpec)
        .claimedVolumes(claimedVolumes)
        .build();
  }
}
