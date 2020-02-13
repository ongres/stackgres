/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.factory.OptionalSubResourceStreamFactory;

@ApplicationScoped
public class ClusterStatefulSetVolumes
    implements OptionalSubResourceStreamFactory<Volume, StackGresClusterContext> {

  @Override
  public Stream<Optional<Volume>> streamOptionalResources(StackGresClusterContext config) {
    return Arrays.asList(ClusterStatefulSetVolumeConfig.values())
        .stream()
        .filter(volumeConfig -> config.getRestoreContext().isPresent()
            || (volumeConfig != ClusterStatefulSetVolumeConfig.RESTORE_CONFIG // NOPMD
            && volumeConfig != ClusterStatefulSetVolumeConfig.RESTORE_SECRET
            && volumeConfig != ClusterStatefulSetVolumeConfig.RESTORE_ENTRYPOINT))
        .map(ClusterStatefulSetVolumeConfig::volumeFactory)
        .map(volumeConfigFactory -> volumeConfigFactory.apply(config));
  }

}
