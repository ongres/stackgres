/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Volume;

public interface ContainerFactory<T> {

  default boolean isActivated(T context) {
    return true;
  }

  Container getContainer(T context);

  List<Volume> getVolumes(T context);

  Map<String, String> getComponentVersions(T context);
}
