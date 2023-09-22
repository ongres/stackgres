/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsResources;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractContainerProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;

@Singleton
@OperatorVersionBinder
public class DistributedLogsStatefulSetContainerProfileDecorator extends
    AbstractContainerProfileDecorator implements Decorator<StackGresDistributedLogsContext> {

  @Override
  protected StackGresGroupKind getKind() {
    return StackGresGroupKind.CLUSTER;
  }

  @Override
  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  public HasMetadata decorate(StackGresDistributedLogsContext context,
      HasMetadata resource) {
    if (Optional.of(context.getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getNonProductionOptions)
        .map(StackGresDistributedLogsNonProduction::getDisableClusterResourceRequirements)
        .orElse(false)) {
      return resource;
    }

    if (resource instanceof StatefulSet statefulSet) {
      setProfileContainers(context.getProfile(),
          () -> Optional.of(statefulSet)
          .map(StatefulSet::getSpec)
          .map(StatefulSetSpec::getTemplate)
          .map(PodTemplateSpec::getSpec),
          Optional.ofNullable(context.getSource().getSpec().getResources())
          .map(StackGresDistributedLogsResources::getEnableClusterLimitsRequirements)
          .orElse(false),
          Optional.ofNullable(context.getSource().getSpec().getNonProductionOptions())
          .map(StackGresDistributedLogsNonProduction::getEnableSetClusterCpuRequests)
          .orElse(false),
          Optional.ofNullable(context.getSource().getSpec().getNonProductionOptions())
          .map(StackGresDistributedLogsNonProduction::getEnableSetClusterMemoryRequests)
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
