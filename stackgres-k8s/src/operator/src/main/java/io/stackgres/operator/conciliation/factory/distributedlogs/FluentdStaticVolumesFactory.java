/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.stream.Stream;

import javax.inject.Singleton;

import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.StaticVolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V11)
public class FluentdStaticVolumesFactory
    implements StaticVolumeFactory<StackGresDistributedLogsContext> {

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresDistributedLogsContext context) {
    return Stream.of(
        emptyDir(FluentdStaticVolume.FLUENTD.getVolumeName()),
        emptyDir(FluentdStaticVolume.FLUENTD_BUFFER.getVolumeName()),
        emptyDir(FluentdStaticVolume.FLUENTD_LOG.getVolumeName())
    );
  }

}
