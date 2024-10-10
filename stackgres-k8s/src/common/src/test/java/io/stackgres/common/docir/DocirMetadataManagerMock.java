/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.net.URI;
import java.util.Map;

import io.stackgres.common.WebClientFactory;
import io.stackgres.testutil.JsonUtil;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

@Singleton
@Alternative
@Priority(1)
public class DocirMetadataManagerMock extends DocirMetadataManager {

  static final DocirMetadataManager INSTANCE =
      new DocirMetadataManagerMock();

  public DocirMetadataManagerMock() {
    super(
        JsonUtil.jsonMapper(),
        new WebClientFactory(),
        () -> URI.create("http://0.0.0.0:5001"),
        () -> Map.of(),
        false);
  }

}
