/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.resource.CustomResourceFinder;

//@Mock
public class MockBackupConfigFinder implements CustomResourceFinder<StackGresBackupConfig> {
  @Override
  public Optional<StackGresBackupConfig> findByNameAndNamespace(String name, String namespace) {
    return Optional.empty();
  }
}
