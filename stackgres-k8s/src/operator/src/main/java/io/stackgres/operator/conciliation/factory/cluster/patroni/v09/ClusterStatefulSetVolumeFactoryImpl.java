/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v09;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterStatefulSetVolumeFactory;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
public class ClusterStatefulSetVolumeFactoryImpl
    implements ClusterStatefulSetVolumeFactory<StackGresClusterContext> {

  @Override
  public List<Volume> buildVolumes(StackGresClusterContext context) {
    return ClusterStatefulSetVolumeConfig.volumes(context)
        .collect(Collectors.toUnmodifiableList());
  }
}
