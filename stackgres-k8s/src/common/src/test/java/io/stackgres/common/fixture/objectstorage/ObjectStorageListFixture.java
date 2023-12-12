/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.objectstorage;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorageList;
import io.stackgres.testutil.fixture.Fixture;

public class ObjectStorageListFixture extends Fixture<StackGresObjectStorageList> {

  public ObjectStorageListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_OBJECT_STORAGE_LIST_JSON);
    return this;
  }

  public ObjectStorageListFixture withJustFirstElement() {
    if (fixture.getItems() != null && !fixture.getItems().isEmpty()) {
      fixture.setItems(fixture.getItems().subList(0, 1));
    }
    return this;
  }

}
