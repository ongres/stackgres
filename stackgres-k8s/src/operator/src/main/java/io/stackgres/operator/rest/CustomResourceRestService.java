/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.List;

import io.fabric8.kubernetes.client.CustomResource;

public interface CustomResourceRestService<R extends CustomResource> {

  List<R> list();

  R get(String namespace, String name);

  void create(R resource);

  void delete(R resource);

  void update(R resource);

}
