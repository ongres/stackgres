/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DocirUtilTest {

  private static final URI REPOSITORY = URI.create("https://sgcr.dev");

  @AfterEach
  void tearDown() {
    System.clearProperty(OperatorProperty.USE_PUBLISHED_IMAGES.getPropertyName());
  }

  @Test
  void catalogApiUris_shouldFilterByOperatorVersionAndPublishedImages() {
    for (URI uri : List.of(
        DocirUtil.getBaseImagesApiUri(REPOSITORY),
        DocirUtil.getVersionsApiUri(REPOSITORY),
        DocirUtil.getAddonsApiUri(REPOSITORY),
        DocirUtil.getExtensionsApiUri(REPOSITORY))) {
      assertTrue(uri.getPath().startsWith(DocirUtil.API_PATH + "/"), uri.toString());
      assertEquals(
          DocirUtil.OPERATOR_VERSION_PARAMETER + "=" + StackGresVersion.LATEST.getVersion()
          + "&" + DocirUtil.PUBLISHED_PARAMETER + "=true",
          uri.getQuery(),
          uri.toString());
    }
  }

  @Test
  void catalogApiUris_shouldNotFilterPublishedImagesWhenDisabled() {
    System.setProperty(OperatorProperty.USE_PUBLISHED_IMAGES.getPropertyName(), "false");

    final URI uri = DocirUtil.getVersionsApiUri(REPOSITORY);

    assertEquals(
        DocirUtil.OPERATOR_VERSION_PARAMETER + "=" + StackGresVersion.LATEST.getVersion(),
        uri.getQuery());
    assertFalse(uri.getQuery().contains(DocirUtil.PUBLISHED_PARAMETER));
  }

  @Test
  void operatorVersion_shouldBeMajorMinor() {
    assertTrue(DocirUtil.getOperatorVersion().matches("\\d+\\.\\d+"),
        DocirUtil.getOperatorVersion());
  }

  @Test
  void imageUrlApiUri_shouldNotBeFiltered() {
    assertNull(DocirUtil.getImageUrlApiUri(REPOSITORY).getQuery());
  }

}
