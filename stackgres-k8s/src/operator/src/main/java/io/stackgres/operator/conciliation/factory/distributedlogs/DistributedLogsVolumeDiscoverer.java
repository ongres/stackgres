/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;

@ApplicationScoped
public class DistributedLogsVolumeDiscoverer
    extends ResourceDiscoverer<VolumeFactory<DistributedLogsContext>>
    implements VolumeDiscoverer<DistributedLogsContext> {

  @Inject
  public DistributedLogsVolumeDiscoverer(
      @Any Instance<VolumeFactory<DistributedLogsContext>> instance) {
    init(instance);
  }

  @Override
  public Map<String, VolumePair> discoverVolumes(
      DistributedLogsContext context) {

    StackGresVersion version = StackGresVersion.getClusterStackGresVersion(context.getSource());

    return resourceHub.get(version)
        .stream().flatMap(vf -> vf.buildVolumes(context))
        .collect(Collectors.toMap(vp -> vp.getVolume().getName(), Function.identity()));

  }
}
