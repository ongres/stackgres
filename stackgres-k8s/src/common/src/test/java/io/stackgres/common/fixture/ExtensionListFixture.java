/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import java.util.List;

import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.testutil.fixture.Fixture;

public class ExtensionListFixture extends Fixture<List<StackGresClusterExtension>> {

  public ExtensionListFixture loadDefault() {
    fixture = readListFromJson(EXTENSION_METADATA_EXTENSIONS_JSON);
    return this;
  }

}
