/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractContainerCustomVolumeMountDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ClusterStatefulSetContainerCustomVolumeMountDecorator
    extends AbstractContainerCustomVolumeMountDecorator
    implements Decorator<StackGresClusterContext> {

  @Override
  protected StackGresGroupKind getKind() {
    return StackGresGroupKind.CLUSTER;
  }

  @Override
  public HasMetadata decorate(StackGresClusterContext context, HasMetadata resource) {
    if (resource instanceof StatefulSet statefulSet) {
      setCustomVolumeMountContainers(context.getCluster(),
          () -> Optional.of(statefulSet)
          .map(StatefulSet::getSpec)
          .map(StatefulSetSpec::getTemplate)
          .map(PodTemplateSpec::getSpec));
    }

    return resource;
  }

}
