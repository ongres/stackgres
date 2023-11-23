/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BackupConfigFactoryProvider
    implements DefaultFactoryProvider<DefaultCustomResourceFactory<StackGresBackupConfig>> {

  private final DefaultCustomResourceFactory<StackGresBackupConfig> factory;

  @Inject
  public BackupConfigFactoryProvider(DefaultCustomResourceFactory<StackGresBackupConfig> factory) {
    this.factory = factory;
  }

  @Override
  public List<DefaultCustomResourceFactory<StackGresBackupConfig>> getFactories() {
    return ImmutableList.of(factory);
  }
}
