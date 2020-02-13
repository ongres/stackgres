/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterStatefulSetVolumeMounts
    implements SubResourceStreamFactory<VolumeMount, StackGresClusterContext> {

  @Override
  public Stream<VolumeMount> streamResources(StackGresClusterContext config) {
    return Seq.<VolumeMount>empty()
        .append(Seq.of(ClusterStatefulSetVolumeConfig.values())
            .filter(volumeConfig -> config.getRestoreContext().isPresent()
                || (volumeConfig != ClusterStatefulSetVolumeConfig.RESTORE_CONFIG  // NOPMD
                && volumeConfig != ClusterStatefulSetVolumeConfig.RESTORE_SECRET
                && volumeConfig != ClusterStatefulSetVolumeConfig.RESTORE_ENTRYPOINT))
            .map(ClusterStatefulSetVolumeConfig::volumeMountFactory)
            .map(volumeMountFactory -> volumeMountFactory.apply(config)));
  }

}
