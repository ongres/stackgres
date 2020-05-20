/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb;

import java.util.List;

import io.stackgres.apiweb.distributedlogs.dto.ResourceDto;

public interface ResourceRestService<T extends ResourceDto> {

  List<T> list();

  T get(String namespace, String name);

  void create(T resource);

  void delete(T resource);

  void update(T resource);

}
