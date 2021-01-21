/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;

import io.fabric8.kubernetes.api.model.Volume;

public interface ClusterStatefulSetVolumeFactory<T> {

  List<Volume> buildVolumes(T context);
}
