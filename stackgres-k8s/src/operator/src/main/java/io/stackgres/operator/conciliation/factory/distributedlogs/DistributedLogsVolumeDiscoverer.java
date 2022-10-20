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

import io.stackgres.common.StackGresGroupKind;
import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;

@ApplicationScoped
public class DistributedLogsVolumeDiscoverer
    extends ResourceDiscoverer<VolumeFactory<StackGresDistributedLogsContext>>
    implements VolumeDiscoverer<StackGresDistributedLogsContext> {

  @Inject
  public DistributedLogsVolumeDiscoverer(
      @Any Instance<VolumeFactory<StackGresDistributedLogsContext>> instance) {
    init(instance);
  }

  @Override
  public Map<String, VolumePair> discoverVolumes(
      StackGresDistributedLogsContext context) {
    return resourceHub.get(context.getVersion())
        .stream()
        .filter(vf -> vf.kind() == StackGresGroupKind.CLUSTER)
        .flatMap(vf -> vf.buildVolumes(context))
        .collect(Collectors.toMap(vp -> vp.getVolume().getName(), Function.identity()));

  }
}
