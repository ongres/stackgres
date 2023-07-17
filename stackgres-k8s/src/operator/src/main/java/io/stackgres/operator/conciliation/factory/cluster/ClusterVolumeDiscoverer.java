/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.stackgres.common.StackGresGroupKind;
import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterVolumeDiscoverer
    extends ResourceDiscoverer<VolumeFactory<StackGresClusterContext>>
    implements VolumeDiscoverer<StackGresClusterContext> {

  @Inject
  public ClusterVolumeDiscoverer(
      @Any Instance<VolumeFactory<StackGresClusterContext>> instance) {
    init(instance);
  }

  @Override
  public Map<String, VolumePair> discoverVolumes(
      StackGresClusterContext context) {
    return resourceHub.get(context.getVersion())
        .stream()
        .filter(vf -> vf.kind() == StackGresGroupKind.CLUSTER)
        .flatMap(vf -> vf.buildVolumes(context))
        .collect(Collectors.toMap(vp -> vp.getVolume().getName(), Function.identity()));
  }

}
