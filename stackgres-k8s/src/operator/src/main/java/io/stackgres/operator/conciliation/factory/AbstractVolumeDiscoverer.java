/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.operator.conciliation.AbstractDiscoverer;
import io.stackgres.operator.conciliation.GenerationContext;
import jakarta.enterprise.inject.Instance;

public abstract class AbstractVolumeDiscoverer<T extends GenerationContext<?>>
    extends AbstractDiscoverer<VolumeFactory<T>>
    implements VolumeDiscoverer<T> {

  protected AbstractVolumeDiscoverer(Instance<VolumeFactory<T>> instance) {
    super(instance);
  }

  public AbstractVolumeDiscoverer() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  @Override
  public Map<String, VolumePair> discoverVolumes(T context) {
    return hub.get(context.getVersion())
        .stream()
        .filter(vf -> vf.kind() == StackGresGroupKind.CLUSTER)
        .flatMap(vf -> vf.buildVolumes(context))
        .collect(Collectors.toMap(vp -> vp.getVolume().getName(), Function.identity()));
  }

}
