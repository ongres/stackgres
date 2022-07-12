/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.storage.StorageClassList;
import io.fabric8.kubernetes.api.model.storage.StorageClassListBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class StorageClassListFixture extends Fixture<StorageClassList> {

  public StorageClassListFixture loadDefault() {
    fixture = readFromJson(STORAGE_CLASS_LIST_JSON);
    return this;
  }

  public StorageClassListBuilder getBuilder() {
    return new StorageClassListBuilder(fixture);
  }

}
