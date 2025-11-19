/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.KubernetesVersionBinder;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@KubernetesVersionBinder(to = "1.22")
public class ClusterStatefulSetK8sV1M22 extends ClusterStatefulSet {

  @Inject
  public ClusterStatefulSetK8sV1M22(
      LabelFactoryForCluster labelFactory,
      PodTemplateFactoryDiscoverer<ClusterContainerContext>
          podTemplateSpecFactoryDiscoverer,
      VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer) {
    super(labelFactory, podTemplateSpecFactoryDiscoverer, volumeDiscoverer);
  }

  @Override
  protected void applyToStatefulSetBuilder(StatefulSetBuilder clusterStatefulSetBuilder) {
  }

}
