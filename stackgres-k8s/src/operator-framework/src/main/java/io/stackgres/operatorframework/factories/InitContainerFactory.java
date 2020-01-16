/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.factories;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Container;

public interface InitContainerFactory<T> {

  ImmutableList<Container> getInitContainers(T config);

}
