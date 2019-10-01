/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import java.util.Optional;

public interface KubernetesResourceFinder<T> {

  Optional<T> findByName(String name);

}
