/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class SecretFixture extends Fixture<Secret> {

  public SecretFixture loadDefault() {
    fixture = readFromJson(SECRET_SECRET_JSON);
    return this;
  }

  public SecretFixture loadPatroni() {
    fixture = readFromJson(SECRET_PATRONI_JSON);
    return this;
  }

  public SecretFixture loadBackup() {
    fixture = readFromJson(SECRET_BACKUP_SECRET_JSON);
    return this;
  }

  public SecretFixture loadBackupWithManagedFields() {
    fixture = readFromJson(SECRET_BACKUP_SECRET_WITH_MANAGED_FIELDS_JSON);
    return this;
  }

  public SecretFixture loadMinio() {
    fixture = readFromJson(SECRET_MINIO_JSON);
    return this;
  }

  public SecretFixture loadAuthentication() {
    fixture = readFromJson(SECRET_AUTHENTICATION_JSON);
    return this;
  }

  public SecretFixture loadUser() {
    fixture = readFromJson(SECRET_USER_JSON);
    return this;
  }

  public SecretBuilder getBuilder() {
    return new SecretBuilder(fixture);
  }

}
