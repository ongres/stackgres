/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.objectstorage;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.testutil.fixture.Fixture;

public class ObjectStorageFixture extends Fixture<StackGresObjectStorage> {

  public ObjectStorageFixture loadDefault() {
    fixture = readFromJson(STACKGRES_OBJECT_STORAGE_DEFAULT_JSON);
    return this;
  }

}
