/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;

public interface ClusterEnvironmentVariablesFactory<T> {

  List<EnvVar> buildEnvironmentVariables(T context);
}
