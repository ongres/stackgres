/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class FluentdStaticVolumesFactory
    implements VolumeFactory<StackGresDistributedLogsContext> {

  @Override
  public @Nonnull Stream<VolumePair> buildVolumes(StackGresDistributedLogsContext context) {
    return Stream.of(
        emptyDir(StackGresVolume.FLUENTD.getName()),
        emptyDir(StackGresVolume.FLUENTD_BUFFER.getName()),
        emptyDir(StackGresVolume.FLUENTD_LOG.getName())
    );
  }

}
