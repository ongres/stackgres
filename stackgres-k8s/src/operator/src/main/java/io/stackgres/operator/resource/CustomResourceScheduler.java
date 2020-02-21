/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import io.fabric8.kubernetes.client.CustomResource;

public interface CustomResourceScheduler<T extends CustomResource> {

  void create(T resource);

  void update(T resource);

  void delete(T resource);

}
