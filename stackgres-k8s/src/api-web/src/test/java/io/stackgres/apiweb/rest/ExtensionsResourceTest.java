/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import io.stackgres.apiweb.dto.extension.Extension;
import io.stackgres.apiweb.dto.extension.ExtensionsDto;
import io.stackgres.apiweb.transformer.ExtensionsTransformer;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.extension.StackGresExtension;
import io.stackgres.common.extension.StackGresExtensionVersion;
import io.stackgres.common.extension.StackGresExtensions;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtensionsResourceTest {

  private static final URI REPOSITORY = URI.create("https://extensions.stackgres.io/postgres/repository?skipHostVerification=true");

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedVersions().findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedMajorVersions().findFirst().get();

  private static final String BUILD_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedBuildMajorVersions().findFirst().get();

  @Mock
  private ClusterExtensionMetadataManager clusterExtensionMetadataManager;

  private StackGresExtensions extensions;

  private ExtensionsResource resource;

  @BeforeEach
  void setUp() {
    extensions = JsonUtil
        .readFromJson("extension_metadata/index.json",
            StackGresExtensions.class);
    extensions.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> target.getPostgresVersion().equals("12"))
        .forEach(target -> {
          target.setBuild(BUILD_MAJOR_VERSION);
          target.setPostgresVersion(POSTGRES_MAJOR_VERSION);
        });
    resource = new ExtensionsResource(clusterExtensionMetadataManager,
        new ExtensionsTransformer(clusterExtensionMetadataManager));
  }

  @Test
  void getWithExactVersionShouldReturnAllExtensions() throws Exception {
    when(clusterExtensionMetadataManager.getExtensions()).thenReturn(
        Seq.seq(ExtensionUtil.toExtensionsMetadataIndexAnyVersions(REPOSITORY, extensions))
          .map(Tuple2::v2)
          .flatMap(List::stream)
          .toList());

    ExtensionsDto extensionsDto = resource.get(POSTGRES_VERSION);

    assertThat(extensionsDto.getPublishers(), hasSize(1));
    assertThat(extensionsDto.getExtensions(), hasSize(3));
    assertEquals(1, extensionsDto.getExtensions().stream()
        .filter(extension -> extension.getName().equals("pgsodium"))
        .map(Extension::getVersions).count());
    assertEquals(1, extensionsDto.getExtensions().stream()
        .filter(extension -> extension.getName().equals("mysqlcompat"))
        .map(Extension::getVersions).count());
    assertEquals(1, extensionsDto.getExtensions().stream()
        .filter(extension -> extension.getName().equals("plpgsql"))
        .map(Extension::getVersions).count());
  }

}
