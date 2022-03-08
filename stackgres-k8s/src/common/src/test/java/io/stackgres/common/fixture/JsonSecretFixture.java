/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.testutil.fixture.Fixture;

public class JsonSecretFixture extends Fixture<ObjectNode> {

  public JsonSecretFixture loadBackup() {
    fixture = readFromJsonAsJson(SECRET_BACKUP_SECRET_JSON);
    return this;
  }

  public JsonSecretFixture loadBackupWithManagedFields() {
    fixture = readFromJsonAsJson(SECRET_BACKUP_SECRET_WITH_MANAGED_FIELDS_JSON);
    return this;
  }

}
