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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.apiweb.dto.extension.Extension;
import io.stackgres.apiweb.dto.extension.ExtensionsDto;
import io.stackgres.apiweb.rest.misc.ExtensionsResource;
import io.stackgres.apiweb.transformer.ExtensionsTransformer;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterRegistry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.docir.DocirExtension;
import io.stackgres.common.docir.DocirExtensionIndexAnyVersion;
import io.stackgres.common.docir.DocirMetadataManager;
import io.stackgres.common.docir.DocirUtil;
import io.stackgres.common.docir.StackGresContextMock;
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

  static final String PG_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedVersions(StackGresContextMock.CONTEXT)
      .findAny().get();
  static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
          .streamOrderedMajorVersions(StackGresContextMock.CONTEXT)
      .get(1).get();
  static final String SECOND_PG_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .getVersion(StackGresContextMock.CONTEXT, SECOND_PG_MAJOR_VERSION);
  static final String BUILD_REVISION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedTagVersions(StackGresContextMock.CONTEXT)
      .findFirst().get().getRevision().toString();
  static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .getBuildVersion(StackGresContextMock.CONTEXT, PG_VERSION);
  static final String BUILD_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .getBuildMajorVersion(StackGresContextMock.CONTEXT, PG_VERSION);

  @Mock
  private StackGresContext context;

  @Mock
  private DocirMetadataManager docirMetadataManager;

  private List<DocirExtension> extensionsMetadata;

  private ExtensionsResource resource;

  @BeforeEach
  void setUp() {
    extensionsMetadata = Fixtures.docirExtensionsMetadata().loadDefault().get();
    extensionsMetadata.stream()
        .map(DocirExtension::getVersions)
        .flatMap(List::stream)
        .forEach(target -> target.setRevision(BUILD_REVISION));
    extensionsMetadata.stream()
        .map(DocirExtension::getVersions)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getFlavorVersion(), "12.4"))
        .forEach(target -> target.setFlavorVersion(PG_VERSION));
    extensionsMetadata.stream()
        .map(DocirExtension::getVersions)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getFlavorVersion(), "11.9"))
        .forEach(target -> target.setFlavorVersion(SECOND_PG_VERSION));
    resource = new ExtensionsResource(
        docirMetadataManager,
        new ExtensionsTransformer(
            context,
            docirMetadataManager));
    lenient().when(context.getMetadataManager())
        .thenReturn(docirMetadataManager);
    lenient().when(docirMetadataManager.getFlavors())
        .thenReturn(StackGresContextMock.CONTEXT.getMetadataManager().getFlavors());
    lenient().when(docirMetadataManager.getFlavors(nullable(URI.class)))
        .thenReturn(StackGresContextMock.CONTEXT.getMetadataManager().getFlavors());
  }

  @Test
  void getWithExactVersionShouldReturnAllExtensions() throws Exception {
    when(docirMetadataManager.getExtensions()).thenReturn(
        Seq.seq(DocirUtil.toExtensionsMetadataIndex(extensionsMetadata))
          .map(Tuple2::v2)
          .toList());
    when(docirMetadataManager.getExtensionsAnyVersion(
        any(), any(), eq(getClusterExtension("pgsodium")), anyBoolean())).thenReturn(
            DocirUtil.toExtensionsMetadataIndexAnyVersions(extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("pgsodium"))));
    when(docirMetadataManager.getExtensionsAnyVersion(
        any(), any(), eq(getClusterExtension("mysqlcompat")), anyBoolean())).thenReturn(
            DocirUtil.toExtensionsMetadataIndexAnyVersions(extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("mysqlcompat"))));
    when(docirMetadataManager.getExtensionsAnyVersion(
        any(), any(), eq(getClusterExtension("plpgsql")), anyBoolean())).thenReturn(
            DocirUtil.toExtensionsMetadataIndexAnyVersions(extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("plpgsql"))));

    ExtensionsDto extensionsDto = resource.get(PG_VERSION, null);

    assertThat(extensionsDto.getPublishers(), hasSize(0));
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

    verify(docirMetadataManager, times(1)).getExtensions();
    verify(docirMetadataManager, times(3)).getExtensionsAnyVersion(
        any(), any(), any(), anyBoolean());
  }

  @Test
  void getWithExactVersionShouldReturnAllExtensionsButUniqueVersions() throws Exception {
    DocirExtension sameVersionWithAnotherBuildExtension = JsonUtil
        .fromJson(JsonUtil.toJson(extensionsMetadata.stream()
            .filter(extension -> extension.getName().equals("mysqlcompat"))
            .findFirst()
            .get()), DocirExtension.class);
    Seq.seq(sameVersionWithAnotherBuildExtension.getVersions())
        .zipWithIndex()
        .forEach(t -> t.v1.setRevision(
            String.valueOf(99999990 + Math.min(9, t.v2.intValue()))));
    extensionsMetadata.add(sameVersionWithAnotherBuildExtension);

    when(docirMetadataManager.getExtensions()).thenReturn(
        Seq.seq(DocirUtil.toExtensionsMetadataIndex(extensionsMetadata))
          .map(Tuple2::v2)
          .toList());
    when(docirMetadataManager.getExtensionsAnyVersion(
        any(), any(), eq(getClusterExtension("pgsodium")), anyBoolean())).thenReturn(
            DocirUtil.toExtensionsMetadataIndexAnyVersions(extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("pgsodium"))));
    when(docirMetadataManager.getExtensionsAnyVersion(
        any(), any(), eq(getClusterExtension("mysqlcompat")), anyBoolean())).thenReturn(
            DocirUtil.toExtensionsMetadataIndexAnyVersions(extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("mysqlcompat"))));
    when(docirMetadataManager.getExtensionsAnyVersion(
        any(), any(), eq(getClusterExtension("plpgsql")), anyBoolean())).thenReturn(
            DocirUtil.toExtensionsMetadataIndexAnyVersions(extensionsMetadata)
            .get(getIndexAnyVersion(PG_VERSION, getClusterExtension("plpgsql"))));

    ExtensionsDto extensionsDto = resource.get(PG_VERSION, null);

    assertThat(extensionsDto.getPublishers(), hasSize(0));
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
    return extensionsMetadata.stream()
        .filter(extension -> extension.getName().equals(name))
        .map(extension -> {
          StackGresClusterExtension clusterExtension = new StackGresClusterExtension();
          clusterExtension.setName(extension.getName());
          clusterExtension.setRepository(extension.getRepository());
          return clusterExtension;
        })
        .findAny()
        .get();
  }

  private DocirExtensionIndexAnyVersion getIndexAnyVersion(
      String postgresVersion,
      StackGresClusterExtension clusterExtension) {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setMetadata(new ObjectMeta());
    cluster.getMetadata().setAnnotations(
        Map.of(StackGresKeys.VERSION_KEY, StackGresVersion.LATEST.getVersion()));
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    cluster.getSpec().getPostgres().setVersion(postgresVersion);
    cluster.getSpec().setConfigurations(new StackGresClusterConfigurations());
    cluster.getSpec().getConfigurations().setRegistry(new StackGresClusterRegistry());
    cluster.getSpec().getConfigurations().getRegistry().setEnabled(true);
    return DocirExtensionIndexAnyVersion.fromClusterExtension(
        StackGresContextMock.CONTEXT, cluster, clusterExtension, false);
  }

}
