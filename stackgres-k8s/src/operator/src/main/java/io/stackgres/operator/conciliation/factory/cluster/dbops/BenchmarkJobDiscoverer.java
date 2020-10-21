/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.dbops;

import java.util.Map;

import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;

public interface BenchmarkJobDiscoverer {

  Map<String, JobFactory> discoverFactories(StackGresClusterContext context);

}
