/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.storage.StorageClass;

public interface StorageClassFinder {

  Optional<StorageClass> findStorageClass(String name);
}
