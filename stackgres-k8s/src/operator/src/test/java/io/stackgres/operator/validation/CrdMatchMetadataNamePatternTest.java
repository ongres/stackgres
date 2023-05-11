/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import io.stackgres.common.resource.ResourceUtil;

class CrdMatchMetadataNamePatternTest extends CrdMatchTestHelper {

  void stackGresClusterCrd_ShouldHasMetadataNamePatterAttribute()
      throws IOException {
    withEveryYaml(crdTree -> {
      JsonNode metadataNamepattern = extractMetadataNamePattern(crdTree);
      assertNotNull(format("%s need to have metadata.name.pattern attribute",
          crdTree.get("metadata").get("name")),
          metadataNamepattern);
      assertEquals(ResourceUtil.DNS_LABEL_NAME.pattern(), metadataNamepattern.asText());
    });
  }

  private JsonNode extractMetadataNamePattern(JsonNode crdTree) {
    return crdTree.get("spec").get("versions").get(0).get("schema").get("openAPIV3Schema")
        .get("properties").get("metadata")
        .get("properties").get("name").get("pattern");
  }
}
