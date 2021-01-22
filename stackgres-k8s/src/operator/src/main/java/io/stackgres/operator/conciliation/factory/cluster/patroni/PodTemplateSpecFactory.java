/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodAffinityTermBuilder;
import io.fabric8.kubernetes.api.model.PodAntiAffinityBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.InitContainerFactoryDiscover;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import org.jooq.lambda.Seq;

@Singleton
public class PodTemplateSpecFactory
    implements ResourceFactory<StackGresClusterContext, PodTemplateSpec> {

  private final ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityContext;

  private final LabelFactory<StackGresCluster> labelFactory;

  private final ContainerFactoryDiscoverer<StackGresClusterContext> containerFactoryDiscoverer;

  private final InitContainerFactoryDiscover<StackGresClusterContext>
      initContainerFactoryDiscoverer;

  private final ClusterStatefulSetVolumeFactoryDiscoverer<StackGresClusterContext>
      volumeFactoryDiscoverer;

  @Inject
  public PodTemplateSpecFactory(
      ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityContext,
      LabelFactory<StackGresCluster> labelFactory,
      ContainerFactoryDiscoverer<StackGresClusterContext> containerFactoryDiscoverer,
      InitContainerFactoryDiscover<StackGresClusterContext> initContainerFactoryDiscoverer,
      ClusterStatefulSetVolumeFactoryDiscoverer<StackGresClusterContext> volumeFactoryDiscoverer) {
    this.podSecurityContext = podSecurityContext;
    this.labelFactory = labelFactory;
    this.containerFactoryDiscoverer = containerFactoryDiscoverer;
    this.initContainerFactoryDiscoverer = initContainerFactoryDiscoverer;
    this.volumeFactoryDiscoverer = volumeFactoryDiscoverer;
  }

  @Override
  public PodTemplateSpec createResource(StackGresClusterContext context) {

    StackGresCluster cluster = context.getSource();

    final Map<String, String> podLabels = labelFactory.statefulSetPodLabels(cluster);
    final Map<String, String> customPodLabels = posCustomLabels(cluster);

    List<ContainerFactory<StackGresClusterContext>> containerFactories = containerFactoryDiscoverer
        .discoverContainers(context);

    List<Container> containers = containerFactories.stream()
        .map(f -> f.getContainer(context)).collect(Collectors.toUnmodifiableList());

    Map<String, String> componentVersions = containerFactories.stream()
        .map(f -> f.getComponentVersions(context))
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    List<Volume> volumes = containerFactories.stream()
        .flatMap(f -> f.getVolumes(context).stream())
        .collect(Collectors.toUnmodifiableList());

    List<Container> initContainers = initContainerFactoryDiscoverer.discoverContainers(context)
        .stream().map(f -> f.getContainer(context))
        .collect(Collectors.toUnmodifiableList());

    List<Volume> initContainerVolumes = initContainerFactoryDiscoverer.discoverContainers(context)
        .stream().flatMap(f -> f.getVolumes(context).stream())
        .collect(Collectors.toUnmodifiableList());

    return new PodTemplateSpecBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .addToLabels(customPodLabels)
            .addToLabels(podLabels)
            .addToAnnotations(StackGresContext.VERSION_KEY,
                StackGresProperty.OPERATOR_VERSION.getString())
            .addToAnnotations(componentVersions)
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
                                    .withValues(labelFactory.getLabelMapper().appName())
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
                cluster.getSpec().getNonProduction())
                .map(StackGresClusterNonProduction::getDisableClusterPodAntiAffinity)
                .map(disableClusterPodAntiAffinity -> !disableClusterPodAntiAffinity)
                .orElse(true))
            .orElse(null))
        .withNodeSelector(Optional.ofNullable(cluster.getSpec())
            .map(StackGresClusterSpec::getPod)
            .map(StackGresClusterPod::getScheduling)
            .map(StackGresClusterPodScheduling::getNodeSelector)
            .orElse(null))
        .withTolerations(Optional.ofNullable(cluster.getSpec())
            .map(StackGresClusterSpec::getPod)
            .map(StackGresClusterPod::getScheduling)
            .map(StackGresClusterPodScheduling::getTolerations)
            .map(tolerations -> Seq.seq(tolerations)
                .map(TolerationBuilder::new)
                .map(TolerationBuilder::build)
                .toList())
            .orElse(null))
        .withShareProcessNamespace(Boolean.TRUE)
        .withServiceAccountName(PatroniRoleGenerator.roleName(context))
        .withSecurityContext(podSecurityContext.createResource(context))
        .withVolumes(buildVolumes(context))
        .addAllToVolumes(volumes)
        .withContainers(containers)
        .withInitContainers(initContainers)
        .addAllToVolumes(initContainerVolumes)
        .withTerminationGracePeriodSeconds(60L)
        .endSpec()
        .build();
  }

  public Map<String, String> posCustomLabels(StackGresCluster cluster) {
    return Optional.ofNullable(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getMetadata)
        .map(StackGresClusterPodMetadata::getLabels)
        .orElse(ImmutableMap.of());
  }

  protected List<Volume> buildVolumes(StackGresClusterContext context) {
    var volumeFactories = volumeFactoryDiscoverer
        .discoverFactories(context);

    List<Volume> volumes = new ArrayList<>();
    volumeFactories.forEach(volumesFactory -> volumes.addAll(volumesFactory.buildVolumes(context)));

    return volumes;
  }
}
