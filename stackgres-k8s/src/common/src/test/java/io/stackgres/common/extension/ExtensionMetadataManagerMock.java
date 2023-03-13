/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.net.URI;

import io.quarkus.test.Mock;
import io.stackgres.common.fixture.Fixtures;

@Mock
public class ExtensionMetadataManagerMock extends ExtensionMetadataManager {

  public ExtensionMetadataManagerMock() {
    super(null, null);
  }

  @Override
  synchronized ExtensionMetadataCache getExtensionsMetadata() {
    return ExtensionMetadataCache.from(
        URI.create("https://extensions.strackgres.io/postgres/repository"),
        Fixtures.extensionMetadata().loadDefault().get());
  }

}
