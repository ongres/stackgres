/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;

public interface ResourceScanner<T> {

  List<T> findResources();

  List<T> findResourcesInNamespace(String namespace);

}
