/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;

@ApplicationScoped
public class ClusterStatefulSetVolumeMounts
    implements SubResourceStreamFactory<VolumeMount, StackGresClusterContext> {

  @Override
  public Stream<VolumeMount> streamResources(StackGresClusterContext config) {
    return ClusterStatefulSetVolumeConfig.volumeMounts(config);
  }

}
