/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.stackgres.apiweb.dto.extension.Extension;
import io.stackgres.apiweb.dto.extension.ExtensionsDto;
import io.stackgres.apiweb.transformer.ExtensionsTransformer;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.extension.StackGresExtension;
import io.stackgres.common.extension.StackGresExtensionIndexAnyVersion;
import io.stackgres.common.extension.StackGresExtensionVersion;
import io.stackgres.common.extension.StackGresExtensions;
import io.stackgres.common.fixture.Fixtures;
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

  private static final URI REPOSITORY = URI.create("https://extensions.stackgres.io/postgres/repository");

  static final String PG_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
      .findAny().get();
  static final String FIRST_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
      .get(0).get();
  static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
      .get(1).get();
  static final String SECOND_PG_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getVersion(SECOND_PG_MAJOR_VERSION);
  static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getBuildVersion(PG_VERSION);
  static final String BUILD_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getBuildMajorVersion(PG_VERSION);

  @Mock
  private ClusterExtensionMetadataManager clusterExtensionMetadataManager;

  private StackGresExtensions extensionsMetadata;

  private ExtensionsResource resource;

  @BeforeEach
  void setUp() {
    extensionsMetadata = Fixtures.extensionMetadata().loadDefault().get();
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getBuild(), "5.1"))
        .forEach(target -> target.setBuild(BUILD_VERSION));
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .forEach(target -> target.setBuild(
            BUILD_MAJOR_VERSION + "." + target.getBuild().split("\\.")[1]));
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getPostgresVersion(), "12"))
        .forEach(target -> target.setPostgresVersion(FIRST_PG_MAJOR_VERSION));
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getPostgresVersion(), "11"))
        .forEach(target -> target.setPostgresVersion(SECOND_PG_MAJOR_VERSION));
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getPostgresVersion(), "12.4"))
        .forEach(target -> target.setPostgresVersion(PG_VERSION));
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getPostgresVersion(), "11.9"))
        .forEach(target -> target.setPostgresVersion(SECOND_PG_VERSION));
    resource = new ExtensionsResource(clusterExtensionMetadataManager,
        new ExtensionsTransformer(clusterExtensionMetadataManager,
            JsonMapper.builder().build()));
  }

  @Test
  void getWithExactVersionShouldReturnAllExtensions() throws Exception {
    when(clusterExtensionMetadataManager.getExtensions()).thenReturn(
        Seq.seq(ExtensionUtil.toExtensionsMetadataIndex(REPOSITORY, extensionsMetadata))
          .map(Tuple2::v2)
          .toList());
    when(clusterExtensionMetadataManager.getExtensionsAnyVersion(
        any(), eq(getClusterExtension("pgsodium")), anyBoolean())).thenReturn(
            ExtensionUtil.toExtensionsMetadataIndexAnyVersions(REPOSITORY, extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("pgsodium"))));
    when(clusterExtensionMetadataManager.getExtensionsAnyVersion(
        any(), eq(getClusterExtension("mysqlcompat")), anyBoolean())).thenReturn(
            ExtensionUtil.toExtensionsMetadataIndexAnyVersions(REPOSITORY, extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("mysqlcompat"))));
    when(clusterExtensionMetadataManager.getExtensionsAnyVersion(
        any(), eq(getClusterExtension("plpgsql")), anyBoolean())).thenReturn(
            ExtensionUtil.toExtensionsMetadataIndexAnyVersions(REPOSITORY, extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("plpgsql"))));

    ExtensionsDto extensionsDto = resource.get(PG_VERSION, null);

    assertThat(extensionsDto.getPublishers(), hasSize(1));
    assertThat(extensionsDto.getExtensions(), hasSize(3));
    assertEquals(1, extensionsDto.getExtensions().stream()
        .filter(extension -> extension.getName().equals("pgsodium"))
        .map(Extension::getVersions).flatMap(List::stream).count());
    assertEquals(1, extensionsDto.getExtensions().stream()
        .filter(extension -> extension.getName().equals("mysqlcompat"))
        .map(Extension::getVersions).flatMap(List::stream).count());
    assertEquals(1, extensionsDto.getExtensions().stream()
        .filter(extension -> extension.getName().equals("plpgsql"))
        .map(Extension::getVersions).flatMap(List::stream).count());

    verify(clusterExtensionMetadataManager, times(1)).getExtensions();
    verify(clusterExtensionMetadataManager, times(3)).getExtensionsAnyVersion(
        any(), any(), anyBoolean());
  }

  @Test
  void getWithExactVersionShouldReturnAllExtensionsButUniqueVersions() throws Exception {
    StackGresExtension sameVersionWithAnotherBuildExtension = JsonUtil
        .fromJson(JsonUtil.toJson(extensionsMetadata.getExtensions().stream()
            .filter(extension -> extension.getName().equals("mysqlcompat"))
            .findFirst()
            .get()), StackGresExtension.class);
    Seq.seq(sameVersionWithAnotherBuildExtension.getVersions())
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .zipWithIndex()
        .forEach(t -> t.v1.setBuild(BUILD_VERSION
            .replaceFirst(".[0-9]+", ".99999999" + Math.min(9, t.v2.intValue()))));
    extensionsMetadata.getExtensions().add(sameVersionWithAnotherBuildExtension);

    when(clusterExtensionMetadataManager.getExtensions()).thenReturn(
        Seq.seq(ExtensionUtil.toExtensionsMetadataIndex(REPOSITORY, extensionsMetadata))
          .map(Tuple2::v2)
          .toList());
    when(clusterExtensionMetadataManager.getExtensionsAnyVersion(
        any(), eq(getClusterExtension("pgsodium")), anyBoolean())).thenReturn(
            ExtensionUtil.toExtensionsMetadataIndexAnyVersions(REPOSITORY, extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("pgsodium"))));
    when(clusterExtensionMetadataManager.getExtensionsAnyVersion(
        any(), eq(getClusterExtension("mysqlcompat")), anyBoolean())).thenReturn(
            ExtensionUtil.toExtensionsMetadataIndexAnyVersions(REPOSITORY, extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("mysqlcompat"))));
    when(clusterExtensionMetadataManager.getExtensionsAnyVersion(
        any(), eq(getClusterExtension("plpgsql")), anyBoolean())).thenReturn(
            ExtensionUtil.toExtensionsMetadataIndexAnyVersions(REPOSITORY, extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("plpgsql"))));

    ExtensionsDto extensionsDto = resource.get(PG_VERSION, null);

    assertThat(extensionsDto.getPublishers(), hasSize(1));
    assertThat(extensionsDto.getExtensions(), hasSize(3));
    assertEquals(1, extensionsDto.getExtensions().stream()
        .filter(extension -> extension.getName().equals("pgsodium"))
        .map(Extension::getVersions).flatMap(List::stream).count());
    assertEquals(1, extensionsDto.getExtensions().stream()
        .filter(extension -> extension.getName().equals("mysqlcompat"))
        .map(Extension::getVersions).flatMap(List::stream).count());
    assertEquals(1, extensionsDto.getExtensions().stream()
        .filter(extension -> extension.getName().equals("plpgsql"))
        .map(Extension::getVersions).flatMap(List::stream).count());
  }

  private StackGresClusterExtension getClusterExtension(String name) {
    return extensionsMetadata.getExtensions().stream()
        .filter(extension -> extension.getName().equals(name))
        .map(extension -> {
          StackGresClusterExtension clusterExtension = new StackGresClusterExtension();
          clusterExtension.setName(extension.getName());
          clusterExtension.setPublisher(extension.getPublisherOrDefault());
          clusterExtension.setRepository(extension.getRepository());
          return clusterExtension;
        })
        .findAny()
        .get();
  }

  private StackGresExtensionIndexAnyVersion getIndexAnyVersion(String postgresVersion,
      StackGresClusterExtension clusterExtension) {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    cluster.getSpec().getPostgres().setVersion(postgresVersion);
    return StackGresExtensionIndexAnyVersion.fromClusterExtension(cluster, clusterExtension, false);
  }

}
