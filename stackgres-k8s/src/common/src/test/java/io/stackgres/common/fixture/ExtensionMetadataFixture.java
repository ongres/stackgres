/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.stackgres.common.extension.StackGresExtensions;
import io.stackgres.common.extension.StackGresExtensionsBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class ExtensionMetadataFixture extends Fixture<StackGresExtensions> {

  public ExtensionMetadataFixture loadDefault() {
    fixture = readFromJson(EXTENSION_METADATA_INDEX_JSON);
    return this;
  }

  public StackGresExtensionsBuilder getBuilder() {
    return new StackGresExtensionsBuilder(fixture);
  }
}
