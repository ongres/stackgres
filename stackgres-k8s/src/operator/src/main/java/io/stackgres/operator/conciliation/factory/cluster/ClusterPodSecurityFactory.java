/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresClusterContext, PodSecurityContext> {

  @Inject
  public ClusterPodSecurityFactory(OperatorPropertyContext operatorContext) {
    super(operatorContext);
  }

  @Override
  public PodSecurityContext createResource(StackGresClusterContext source) {
    final boolean isIoLimitsSet = Optional.of(source.getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getPersistentVolume)
        .map(StackGresClusterPodsPersistentVolume::getIoLimits)
        .map(ioLimits -> ioLimits.getReadIops() != null
            || ioLimits.getWriteIops() != null
            || ioLimits.getReadMiBps() != null
            || ioLimits.getWriteMiBps() != null)
        .orElse(false);
    PodSecurityContext podSecurityContext = createPodSecurityContext();
    podSecurityContext.setRunAsNonRoot(!isIoLimitsSet);
    podSecurityContext.setFsGroupChangePolicy(
        source.getSource().getSpec().getPods().getPersistentVolume().getFsGroupChangePolicy());
    return podSecurityContext;
  }

}
