/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodAffinityTermBuilder;
import io.fabric8.kubernetes.api.model.PodAntiAffinity;
import io.fabric8.kubernetes.api.model.PodAntiAffinityBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPodScheduling;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
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

  private final ResourceFactory<StackGresDistributedLogsContext, PodSecurityContext> podSecContext;

  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  private final ContainerFactoryDiscoverer<DistributedLogsContainerContext> containerDiscoverer;

  private final InitContainerFactoryDiscover<DistributedLogsContainerContext> initContDiscoverer;

  @Inject
  public DistributedLogsPodTemplateSpecFactory(
      ResourceFactory<StackGresDistributedLogsContext, PodSecurityContext> podSecurityContext,
      LabelFactoryForCluster<StackGresDistributedLogs> labelFactory,
      ContainerFactoryDiscoverer<DistributedLogsContainerContext> containerFactoryDiscoverer,
      InitContainerFactoryDiscover<DistributedLogsContainerContext> iniContainerFactoryDiscoverer) {
    this.podSecContext = podSecurityContext;
    this.labelFactory = labelFactory;
    this.containerDiscoverer = containerFactoryDiscoverer;
    this.initContDiscoverer = iniContainerFactoryDiscoverer;
  }

  @Override
  public PodTemplateResult getPodTemplateSpec(DistributedLogsContainerContext context) {
    StackGresDistributedLogs cluster = context.getDistributedLogsContext().getSource();

    final Map<String, String> podLabels = labelFactory.statefulSetPodLabels(cluster);

    List<ContainerFactory<DistributedLogsContainerContext>> containerFactories =
        containerDiscoverer.discoverContainers(context);

    List<Container> containers = containerFactories.stream()
        .map(f -> f.getContainer(context)).toList();

    final List<ContainerFactory<DistributedLogsContainerContext>> initContainerFactories =
        initContDiscoverer.discoverContainers(context);

    List<Container> initContainers = initContainerFactories
        .stream().map(f -> f.getContainer(context))
        .toList();

    final List<String> claimedVolumes = Stream.concat(containers.stream(), initContainers.stream())
        .flatMap(container -> container.getVolumeMounts().stream())
        .map(VolumeMount::getName)
        .distinct()
        .toList();

    claimedVolumes.forEach(rv -> {
      if (!context.availableVolumes().containsKey(rv) && !context.getDataVolumeName().equals(rv)) {
        throw new IllegalStateException("Volume " + rv + " is required but not available");
      }
    });

    List<Volume> volumes = claimedVolumes.stream()
        .map(volumeName -> context.availableVolumes().get(volumeName))
        .filter(Objects::nonNull)
        .toList();

    final boolean isEnabledClusterPodAntiAffinity = Optional.ofNullable(
        cluster.getSpec().getNonProductionOptions())
        .map(StackGresDistributedLogsNonProduction::getDisableClusterPodAntiAffinity)
        .map(disableClusterPodAntiAffinity -> !disableClusterPodAntiAffinity)
        .orElse(true);

    var podTemplateSpec = new PodTemplateSpecBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .addToLabels(podLabels)
            .addToAnnotations(StackGresContext.VERSION_KEY, cluster.getMetadata().getAnnotations()
                .getOrDefault(StackGresContext.VERSION_KEY,
                    StackGresProperty.OPERATOR_VERSION.getString()))
            .build())
        .withNewSpec()
        .withShareProcessNamespace(Boolean.TRUE)
        .withServiceAccountName(PatroniRole.roleName(context.getDistributedLogsContext()))
        .withSecurityContext(podSecContext.createResource(context.getDistributedLogsContext()))
        .withVolumes(volumes)
        .withContainers(containers)
        .withInitContainers(initContainers)
        .withTerminationGracePeriodSeconds(60L)
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
        .withAffinity(new AffinityBuilder()
            .withNodeAffinity(Optional.ofNullable(cluster.getSpec())
                .map(StackGresDistributedLogsSpec::getScheduling)
                .map(StackGresDistributedLogsPodScheduling::getNodeAffinity)
                .orElse(null))
            .withPodAffinity(Optional.ofNullable(cluster.getSpec())
                .map(StackGresDistributedLogsSpec::getScheduling)
                .map(StackGresDistributedLogsPodScheduling::getPodAffinity)
                .orElse(null))
            .withPodAntiAffinity(Optional.ofNullable(cluster.getSpec())
                .map(StackGresDistributedLogsSpec::getScheduling)
                .map(StackGresDistributedLogsPodScheduling::getPodAntiAffinity)
                .map(PodAntiAffinityBuilder::new)
                .orElseGet(PodAntiAffinityBuilder::new)
                .addAllToRequiredDuringSchedulingIgnoredDuringExecution(Seq.of(
                    new PodAffinityTermBuilder()
                        .withLabelSelector(new LabelSelectorBuilder()
                            .withMatchExpressions(new LabelSelectorRequirementBuilder()
                                .withKey(labelFactory.labelMapper().appKey())
                                .withOperator("In")
                                .withValues(labelFactory.labelMapper().appName())
                                .build(),
                                new LabelSelectorRequirementBuilder()
                                    .withKey(labelFactory.labelMapper().clusterKey(cluster))
                                    .withOperator("In")
                                    .withValues(StackGresContext.RIGHT_VALUE)
                                    .build())
                            .build())
                        .withTopologyKey("kubernetes.io/hostname")
                        .build())
                    .filter(affinity -> isEnabledClusterPodAntiAffinity)
                    .append(Optional.ofNullable(cluster.getSpec())
                        .map(StackGresDistributedLogsSpec::getScheduling)
                        .map(StackGresDistributedLogsPodScheduling::getPodAntiAffinity)
                        .map(PodAntiAffinity::getRequiredDuringSchedulingIgnoredDuringExecution)
                        .stream()
                        .flatMap(List::stream))
                    .toList())
                .build())
            .build())
        .endSpec()
        .build();

    return ImmutablePodTemplateResult.builder()
        .spec(podTemplateSpec)
        .claimedVolumes(claimedVolumes)
        .build();
  }
}
