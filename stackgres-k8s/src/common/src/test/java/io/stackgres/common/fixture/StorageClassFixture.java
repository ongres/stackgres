/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.testutil.fixture.Fixture;

public class StorageClassFixture extends Fixture<StorageClass> {

  public StorageClassFixture loadDefault() {
    fixture = readFromJson(STORAGE_CLASS_STANDARD_JSON);
    return this;
  }

}
