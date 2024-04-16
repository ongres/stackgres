/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;

public interface EnvVarProvider<T> {

  List<EnvVar> getEnvVars(T context);

}
