/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Map;

import io.fabric8.kubernetes.api.model.Volume;

public interface ContainerContext {

  Map<String, Volume> availableVolumes();

  String getDataVolumeName();

}
