/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.net.URI;
import java.util.List;

import io.quarkus.test.Mock;
import io.stackgres.common.fixture.Fixtures;
import jakarta.inject.Singleton;

@Mock
@Singleton
public class ExtensionMetadataManagerMock extends ExtensionMetadataManager {

  List<StackGresExtension> extraExtensions = List.of();

  public ExtensionMetadataManagerMock() {
    super(null, null);
  }

  @Override
  synchronized ExtensionMetadataCache getExtensionsMetadata() {
    return ExtensionMetadataCache.from(
        URI.create("https://extensions.strackgres.io/postgres/repository"),
        Fixtures.extensionMetadata().loadDefault()
        .getBuilder()
        .addAllToExtensions(extraExtensions)
        .build());
  }

  public void setExtraExtensions(List<StackGresExtension> extraExtensions) {
    this.extraExtensions = extraExtensions;
  }

}
