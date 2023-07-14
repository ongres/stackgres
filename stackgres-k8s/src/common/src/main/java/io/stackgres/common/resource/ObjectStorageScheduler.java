/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorageList;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ObjectStorageScheduler
    extends AbstractCustomResourceScheduler<StackGresObjectStorage, StackGresObjectStorageList> {

  public ObjectStorageScheduler() {
    super(StackGresObjectStorage.class, StackGresObjectStorageList.class);
  }

}
