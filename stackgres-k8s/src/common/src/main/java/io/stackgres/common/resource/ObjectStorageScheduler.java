/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorageList;

@ApplicationScoped
public class ObjectStorageScheduler
    extends AbstractCustomResourceScheduler<StackGresObjectStorage, StackGresObjectStorageList> {

  public ObjectStorageScheduler() {
    super(StackGresObjectStorage.class, StackGresObjectStorageList.class);
  }

}
