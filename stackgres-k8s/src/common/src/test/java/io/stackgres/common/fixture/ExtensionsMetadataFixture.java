/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import java.util.List;

import io.stackgres.common.docir.DocirExtension;
import io.stackgres.testutil.fixture.Fixture;

public class ExtensionsMetadataFixture extends Fixture<List<DocirExtension>> {

  public ExtensionsMetadataFixture loadDefault() {
    fixture = readListFromJson(DOCIR_METADATA_EXTENSIONS_JSON);
    return this;
  }

}
