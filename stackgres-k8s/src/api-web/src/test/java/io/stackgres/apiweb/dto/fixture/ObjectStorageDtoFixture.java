/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.testutil.fixture.Fixture;

public class ObjectStorageDtoFixture extends Fixture<ObjectStorageDto> {

  public ObjectStorageDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_OBJECT_STORAGE_DTO_JSON);
    return this;
  }

  public ObjectStorageDtoFixture loadGoogleIdentityConfig() {
    fixture = readFromJson(STACKGRES_OBJECT_STORAGE_GOOGLE_IDENTITY_CONFIG_JSON);
    return this;
  }

}
