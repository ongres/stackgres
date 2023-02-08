/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.storages.BackupStorage;

@ApplicationScoped
public class DefaultObjectStorageFactory
    extends AbstractCustomResourceFactory<StackGresObjectStorage> {

  public static final String OBJECT_STORAGE_DEFAULT_VALUES =
      "/object-storage-default-values.properties";

  @Inject
  public DefaultObjectStorageFactory(StackGresPropertyContext<OperatorProperty> context) {
    super(context);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  Properties getDefaultPropertiesFile() {
    return StackGresUtil.loadProperties(OBJECT_STORAGE_DEFAULT_VALUES);
  }

  @Override
  StackGresObjectStorage buildResource(String namespace) {
    StackGresObjectStorage storage = new StackGresObjectStorage();
    storage.getMetadata().setName(generateDefaultName());
    storage.getMetadata().setNamespace(namespace);

    BackupStorage spec = buildFromDefaults(BackupStorage.class);
    storage.setSpec(spec);
    return storage;
  }
}
