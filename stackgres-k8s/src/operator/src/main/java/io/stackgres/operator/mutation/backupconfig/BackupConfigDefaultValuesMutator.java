/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backupconfig;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractValuesMutator;

@ApplicationScoped
public class BackupConfigDefaultValuesMutator
    extends AbstractValuesMutator<StackGresBackupConfig, BackupConfigReview>
    implements BackupConfigMutator {

  @Inject
  public BackupConfigDefaultValuesMutator(
      DefaultCustomResourceFactory<StackGresBackupConfig> factory,
      ObjectMapper jsonMapper) {
    super(factory, jsonMapper);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  public StackGresBackupConfig mutate(
      BackupConfigReview review, StackGresBackupConfig resource) {
    if (resource.getSpec().getStorage() != null
        && resource.getSpec().getStorage().getType() != null
        && !resource.getSpec().getStorage().getType().equals(
            defaultValue.getSpec().getStorage().getType())) {
      return resource;
    }
    return super.mutate(review, resource);
  }

  @Override
  protected Class<StackGresBackupConfig> getResourceClass() {
    return StackGresBackupConfig.class;
  }

}
