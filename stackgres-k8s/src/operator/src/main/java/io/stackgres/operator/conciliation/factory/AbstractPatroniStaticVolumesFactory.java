/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.stream.Stream;

import io.stackgres.common.StackGresVolume;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractPatroniStaticVolumesFactory<T> implements VolumeFactory<T> {

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(T context) {
    return Stream.of(
        inMemoryDir(StackGresVolume.POSTGRES_SOCKET.getName()),
        inMemoryDir(StackGresVolume.DSHM.getName()),
        emptyDir(StackGresVolume.SHARED.getName()),
        emptyDir(StackGresVolume.EMPTY_BASE.getName()),
        emptyDir(StackGresVolume.USER.getName()),
        emptyDir(StackGresVolume.LOCAL_BIN.getName()),
        emptyDir(StackGresVolume.LOG.getName()),
        emptyDir(StackGresVolume.PATRONI_CONFIG.getName()),
        emptyDir(StackGresVolume.POSTGRES_SSL_COPY.getName())
    );
  }
}
