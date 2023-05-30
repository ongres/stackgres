/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

public interface ClusterEnvironmentVariablesFactoryDiscoverer<T> {

  List<ClusterEnvironmentVariablesFactory<T>> discoverFactories(T context);
}
