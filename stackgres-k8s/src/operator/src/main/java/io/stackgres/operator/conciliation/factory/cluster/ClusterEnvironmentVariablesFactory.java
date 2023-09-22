/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.ClusterContext;

public interface ClusterEnvironmentVariablesFactory {

  List<EnvVar> buildEnvironmentVariables(ClusterContext context);

}
