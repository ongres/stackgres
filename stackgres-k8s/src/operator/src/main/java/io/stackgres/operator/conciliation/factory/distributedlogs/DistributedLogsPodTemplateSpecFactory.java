/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPodScheduling;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.InitContainerFactoryDiscover;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.distributedlogs.patroni.PatroniRole;
import org.jooq.lambda.Seq;

@Singleton
public class DistributedLogsPodTemplateSpecFactory
    implements ResourceFactory<DistributedLogsContext, PodTemplateSpec> {

  private final ResourceFactory<DistributedLogsContext, PodSecurityContext> podSecurityContext;

  private final LabelFactory<StackGresDistributedLogs> labelFactory;

  private final ContainerFactoryDiscoverer<DistributedLogsContext> containerFactoryDiscoverer;

  private final InitContainerFactoryDiscover<DistributedLogsContext>
      initContainerFactoryDiscoverer;

  @Inject
  public DistributedLogsPodTemplateSpecFactory(
      ResourceFactory<DistributedLogsContext, PodSecurityContext> podSecurityContext,
      LabelFactory<StackGresDistributedLogs> labelFactory,
      ContainerFactoryDiscoverer<DistributedLogsContext> containerFactoryDiscoverer,
      InitContainerFactoryDiscover<DistributedLogsContext> initContainerFactoryDiscoverer) {
    this.podSecurityContext = podSecurityContext;
    this.labelFactory = labelFactory;
    this.containerFactoryDiscoverer = containerFactoryDiscoverer;
    this.initContainerFactoryDiscoverer = initContainerFactoryDiscoverer;
  }

  @Override
  public PodTemplateSpec createResource(DistributedLogsContext context) {

    StackGresDistributedLogs cluster = context.getSource();

    final Map<String, String> podLabels = labelFactory.statefulSetPodLabels(cluster);

    List<ContainerFactory<DistributedLogsContext>> containerFactories = containerFactoryDiscoverer
        .discoverContainers(context);

    List<Container> containers = containerFactories.stream()
        .map(f -> f.getContainer(context)).collect(Collectors.toUnmodifiableList());

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
            .addToLabels(podLabels)
            .addToAnnotations(StackGresContext.VERSION_KEY,
                StackGresProperty.OPERATOR_VERSION.getString())
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
        .withServiceAccountName(PatroniRole.roleName(context))
        .withSecurityContext(podSecurityContext.createResource(context))
        .addAllToVolumes(volumes)
        .withContainers(containers)
        .withInitContainers(initContainers)
        .addAllToVolumes(initContainerVolumes)
        .withTerminationGracePeriodSeconds(60L)
        .endSpec()
        .build();
  }

}
