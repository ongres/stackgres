/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.v09;

import java.util.stream.Stream;

import javax.inject.Singleton;

import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolumesFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.v09.PatroniStaticVolume;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
public class ClusterPatroniStaticVolumesFactory
    extends PatroniStaticVolumesFactory<StackGresClusterContext> {

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        inMemoryDir(PatroniStaticVolume.POSTGRES_SOCKET.getVolumeName()),
        inMemoryDir(PatroniStaticVolume.DSHM.getVolumeName()),
        emptyDir(PatroniStaticVolume.SHARED.getVolumeName()),
        emptyDir(PatroniStaticVolume.EMPTY_BASE.getVolumeName()),
        emptyDir(PatroniStaticVolume.USER.getVolumeName()),
        emptyDir(PatroniStaticVolume.LOCAL.getVolumeName()),
        emptyDir(PatroniStaticVolume.LOG.getVolumeName()),
        emptyDir(PatroniStaticVolume.PATRONI_CONFIG.getVolumeName())
    );
  }
}
