/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.objectstorage;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorageBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class ObjectStorageFixture extends VersionedFixture<StackGresObjectStorage> {

  public ObjectStorageFixture loadDefault() {
    fixture = readFromJson(STACKGRES_OBJECT_STORAGE_DEFAULT_JSON);
    return this;
  }

  public StackGresObjectStorageBuilder getBuilder() {
    return new StackGresObjectStorageBuilder(fixture);
  }

}
