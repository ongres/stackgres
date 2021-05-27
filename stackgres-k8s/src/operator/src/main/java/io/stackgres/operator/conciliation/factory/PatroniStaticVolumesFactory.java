/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

public abstract class PatroniStaticVolumesFactory<T> implements StaticVolumeFactory<T> {

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(T context) {
    return Stream.of(
        inMemoryDir(PatroniStaticVolume.POSTGRES_SOCKET.getVolumeName()),
        inMemoryDir(PatroniStaticVolume.DSHM.getVolumeName()),
        emptyDir(PatroniStaticVolume.SHARED.getVolumeName()),
        emptyDir(PatroniStaticVolume.EMPTY_BASE.getVolumeName()),
        emptyDir(PatroniStaticVolume.USER.getVolumeName()),
        emptyDir(PatroniStaticVolume.LOCAL_BIN.getVolumeName()),
        emptyDir(PatroniStaticVolume.LOG.getVolumeName()),
        emptyDir(PatroniStaticVolume.PATRONI_CONFIG.getVolumeName())
    );
  }
}
