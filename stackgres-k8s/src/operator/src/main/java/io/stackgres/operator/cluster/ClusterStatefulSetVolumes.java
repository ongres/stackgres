/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.Arrays;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;

@ApplicationScoped
public class ClusterStatefulSetVolumes
    implements SubResourceStreamFactory<Volume, StackGresClusterContext> {

  @Override
  public Stream<Volume> streamResources(StackGresClusterContext config) {
    return ResourceGenerator
        .with(config)
        .of(Volume.class)
        .append(context -> (Stream<Volume>) Arrays.asList(ClusterStatefulSetVolumeConfig.values())
            .stream()
            .filter(config2 -> context.getRestoreContext().isPresent()
                || config2 != ClusterStatefulSetVolumeConfig.RESTORE_ENTRYPOINT)
            .map(config2 -> config2.volumeFactory().apply(context)))
        .stream();
  }

}
