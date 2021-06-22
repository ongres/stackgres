/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;

public interface ClusterStatefulSetVolumeFactoryDiscoverer<T> {

  List<ClusterStatefulSetVolumeFactory<T>> discoverFactories(T context);
}
