/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.net.URI;
import java.util.Map;

import io.stackgres.common.WebClientFactory;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DocirMetadataManagerTest {

  DocirMetadataManager docirMetadataManager = new DocirMetadataManager(
      JsonUtil.jsonMapper(),
      new WebClientFactory(),
      () -> URI.create(DocirConfigUtil.DEFAULT_REPOSITORY_URL),
      () -> Map.of(),
      true) { };

  @Test
  @Disabled("Manual test for feeding the Docir metadata from Docir REST API")
  void getAllExtensions_shouldNotFail() {
    docirMetadataManager.getExtensions();
  }

}
