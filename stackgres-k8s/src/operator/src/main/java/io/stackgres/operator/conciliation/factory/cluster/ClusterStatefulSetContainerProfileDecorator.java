/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractContainerProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;

@Singleton
@OperatorVersionBinder
public class ClusterStatefulSetContainerProfileDecorator extends AbstractContainerProfileDecorator
    implements Decorator<StackGresClusterContext> {

  @Override
  protected StackGresGroupKind getKind() {
    return StackGresGroupKind.CLUSTER;
  }

  @Override
  public HasMetadata decorate(StackGresClusterContext context, HasMetadata resource) {
    if (Optional.of(context.getSource().getSpec())
        .map(StackGresClusterSpec::getNonProductionOptions)
        .map(StackGresClusterNonProduction::getDisableClusterResourceRequirements)
        .orElse(false)) {
      return resource;
    }

    if (resource instanceof StatefulSet statefulSet) {
      setProfileContainers(context.getProfile(),
          () -> Optional.of(statefulSet)
          .map(StatefulSet::getSpec)
          .map(StatefulSetSpec::getTemplate)
          .map(PodTemplateSpec::getSpec),
          Optional.ofNullable(context.getSource().getSpec().getPods().getResources())
          .map(StackGresClusterResources::getEnableClusterLimitsRequirements)
          .orElse(false),
          Optional.ofNullable(context.getSource().getSpec().getNonProductionOptions())
          .map(StackGresClusterNonProduction::getEnableSetClusterCpuRequests)
          .orElse(false),
          Optional.ofNullable(context.getSource().getSpec().getNonProductionOptions())
          .map(StackGresClusterNonProduction::getEnableSetClusterMemoryRequests)
          .orElse(false));
    }

    return resource;
  }

  @Override
  protected void setProfileContainers(StackGresProfile profile,
      Supplier<Optional<PodSpec>> podSpecSupplier,
      boolean enableCpuAndMemoryLimits,
      boolean enableCpuRequests, boolean enableMemoryRequests) {
    podSpecSupplier.get()
        .map(PodSpec::getContainers)
        .stream()
        .flatMap(List::stream)
        .filter(container -> !Objects.equals(
            container.getName(), StackGresContainer.PATRONI.getName()))
        .forEach(container -> setProfileForContainer(profile, podSpecSupplier, container,
            enableCpuAndMemoryLimits, enableCpuRequests, enableMemoryRequests));
    podSpecSupplier.get()
        .map(PodSpec::getInitContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setProfileForInitContainer(profile, podSpecSupplier, container,
            enableCpuAndMemoryLimits, enableCpuRequests, enableMemoryRequests));
  }

}
