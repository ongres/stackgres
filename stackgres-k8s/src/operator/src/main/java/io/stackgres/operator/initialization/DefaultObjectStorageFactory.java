/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.storages.BackupStorage;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultObjectStorageFactory
    extends DefaultCustomResourceFactory<StackGresObjectStorage, HasMetadata> {

  @Override
  protected String getDefaultPropertyResourceName(HasMetadata source) {
    return "/object-storage-default-values.properties";
  }

  @Override
  public StackGresObjectStorage buildResource(HasMetadata resource) {
    StackGresObjectStorage storage = new StackGresObjectStorage();
    storage.getMetadata().setName(getDefaultResourceName(resource));
    storage.getMetadata().setNamespace(resource.getMetadata().getNamespace());

    BackupStorage spec = buildFromDefaults(resource, BackupStorage.class);
    storage.setSpec(spec);
    return storage;
  }
}
