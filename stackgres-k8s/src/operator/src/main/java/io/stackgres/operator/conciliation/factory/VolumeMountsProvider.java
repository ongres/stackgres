/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;

public interface VolumeMountsProvider<T> {

  List<VolumeMount> getVolumeMounts(T context);

  List<EnvVar> getDerivedEnvVars(T context);
}
