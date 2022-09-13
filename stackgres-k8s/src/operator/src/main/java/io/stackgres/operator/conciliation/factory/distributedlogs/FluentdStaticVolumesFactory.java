/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.stream.Stream;

import javax.inject.Singleton;

import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class FluentdStaticVolumesFactory
    implements VolumeFactory<StackGresDistributedLogsContext> {

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresDistributedLogsContext context) {
    return Stream.of(
        emptyDir(StackGresVolume.FLUENTD.getName()),
        emptyDir(StackGresVolume.FLUENTD_BUFFER.getName()),
        emptyDir(StackGresVolume.FLUENTD_LOG.getName())
    );
  }

}
