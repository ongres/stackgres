/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

public interface VolumeFactory<T> {

  @NotNull Stream<VolumePair> buildVolumes(T context);

}
