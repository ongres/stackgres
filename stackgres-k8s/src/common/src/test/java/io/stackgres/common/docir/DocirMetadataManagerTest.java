/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.Map;

import io.stackgres.common.WebClientFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
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

  @Test
  void imageOfCluster_shouldBePulledFromTheRegistryOfTheProxyWhenConfigured() {
    final StackGresCluster cluster = Fixtures.cluster().loadDefault().get();

    final String image = StackGresContextMock.CONTEXT.getMetadataManager()
        .getImage(StackGresContextMock.CONTEXT, cluster);

    cluster.getStatus().setRepository(cluster.getStatus().getRepository()
        + "?proxyUrl=http%3A%2F%2Fstackgres-operator-docir-cache.stackgres"
        + "%3FsetHttpScheme%3Dtrue&retry=3%3A5");

    final String proxiedImage = StackGresContextMock.CONTEXT.getMetadataManager()
        .getImage(StackGresContextMock.CONTEXT, cluster);

    assertEquals(
        "stackgres-operator-docir-cache.stackgres" + image.substring(image.indexOf('/')),
        proxiedImage);
  }

}
