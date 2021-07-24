/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExtensionUtilTest {

  final URI repository =
      URI.create("https://extensions.stackgres.io/postgres/repository?skipHostVerification=true");
  final String pgVersion = StackGresComponent.POSTGRESQL.getOrderedVersions()
      .findAny().get();
  final String firstPgMajorVersion = StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
      .get(0).get();
  final String secondPgMajorVersion = StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
      .get(1).get();
  final String secondPgVersion = StackGresComponent.POSTGRESQL.findVersion(secondPgMajorVersion);
  final String build = StackGresComponent.POSTGRESQL.findBuildVersion(pgVersion);

  StackGresExtensions extensionsMetadata;

  List<StackGresClusterExtension> extensions;

  @BeforeEach
  public void beforeEach() {
    extensionsMetadata = JsonUtil
        .readFromJson("extension_metadata/index.json",
            StackGresExtensions.class);
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getBuild(), "5.1"))
        .forEach(target -> target.setBuild(build));
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getPostgresVersion(), "12"))
        .forEach(target -> target.setPostgresVersion(firstPgMajorVersion));
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getPostgresVersion(), "11"))
        .forEach(target -> target.setPostgresVersion(secondPgMajorVersion));
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getPostgresVersion(), "12.4"))
        .forEach(target -> target.setPostgresVersion(pgVersion));
    extensionsMetadata.getExtensions().stream()
        .map(StackGresExtension::getVersions)
        .flatMap(List::stream)
        .map(StackGresExtensionVersion::getAvailableFor)
        .flatMap(List::stream)
        .filter(target -> Objects.equals(target.getPostgresVersion(), "11.9"))
        .forEach(target -> target.setPostgresVersion(secondPgVersion));
    extensions = JsonUtil
        .readListFromJson("extension_metadata/extensions.json",
            StackGresClusterExtension.class);
  }

  @Test
  public void testExtensionsMetadataMap() {
    StackGresClusterInstalledExtension installedExtension0 =
        new StackGresClusterInstalledExtension();
    installedExtension0.setName(extensions.get(0).getName());
    installedExtension0.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension0.setRepository(repository.toASCIIString());
    installedExtension0.setVersion(extensions.get(0).getVersion());
    installedExtension0.setPostgresVersion(firstPgMajorVersion);
    installedExtension0.setBuild(build);
    StackGresClusterInstalledExtension installedExtension1 =
        new StackGresClusterInstalledExtension();
    installedExtension1.setName(extensions.get(1).getName());
    installedExtension1.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension1.setRepository(repository.toASCIIString());
    installedExtension1.setVersion(extensionsMetadata.getExtensions().stream()
        .filter(e -> e.getName().equals(extensions.get(1).getName())).findAny().get()
        .getVersions().get(0).getVersion());
    installedExtension1.setPostgresVersion(firstPgMajorVersion);
    installedExtension1.setBuild(build);
    StackGresClusterInstalledExtension installedExtension2 =
        new StackGresClusterInstalledExtension();
    installedExtension2.setName(extensions.get(2).getName());
    installedExtension2.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension2.setRepository(repository.toASCIIString());
    installedExtension2.setVersion(extensions.get(2).getVersion());
    installedExtension2.setPostgresVersion(firstPgMajorVersion);
    installedExtension2.setBuild(build);
    StackGresClusterInstalledExtension installedExtension3 =
        new StackGresClusterInstalledExtension();
    installedExtension3.setName(extensions.get(3).getName());
    installedExtension3.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension3.setRepository(repository.toASCIIString());
    installedExtension3.setVersion(extensionsMetadata.getExtensions().stream()
        .filter(e -> e.getName().equals(extensions.get(3).getName())).findAny().get()
        .getVersions().get(0).getVersion());
    installedExtension3.setPostgresVersion(pgVersion);
    installedExtension3.setBuild(build);
    Map<StackGresExtensionIndex, StackGresExtensionMetadata> extensionMetadataMap =
        ExtensionUtil.toExtensionsMetadataIndex(
            repository, extensionsMetadata);
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndex(installedExtension0)));
    Assertions.assertNotEquals(
        repository,
        extensionMetadataMap.get(new StackGresExtensionIndex(installedExtension0))
            .getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndex(installedExtension1)));
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndex(installedExtension1))
            .getExtension().getRepository());
    Assertions.assertNull(extensionMetadataMap.get(
        new StackGresExtensionIndex(installedExtension2)));
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndex(installedExtension3)));
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndex(installedExtension3))
            .getExtension().getRepository());
    installedExtension0.setPostgresVersion(secondPgMajorVersion);
    installedExtension1.setPostgresVersion(secondPgMajorVersion);
    installedExtension2.setPostgresVersion(secondPgMajorVersion);
    installedExtension3.setPostgresVersion(secondPgVersion);
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndex(installedExtension0)));
    Assertions.assertNotEquals(
        repository,
        extensionMetadataMap.get(new StackGresExtensionIndex(installedExtension0))
            .getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndex(installedExtension1)));
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndex(installedExtension1))
            .getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndex(installedExtension2)));
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndex(installedExtension2))
            .getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndex(installedExtension3)));
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndex(installedExtension3))
            .getExtension().getRepository());
  }

  @Test
  public void testExtensionsMetadataMapSameMajorBuild() {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    var extensionMetadataMap = ExtensionUtil.toExtensionsMetadataIndexSameMajorBuilds(
        repository, extensionsMetadata);
    cluster.getSpec().setPostgresVersion(firstPgMajorVersion);
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0))).size());
    Assertions.assertNotEquals(
        repository,
        extensionMetadataMap
            .get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0)))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap
            .get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1)))
            .get(0).getExtension().getRepository());
    Assertions.assertNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(2))));
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(3))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(3))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap
            .get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(3)))
            .get(0).getExtension().getRepository());
    cluster.getSpec().setPostgresVersion(secondPgMajorVersion);
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0))).size());
    Assertions.assertNotEquals(
        repository,
        extensionMetadataMap
            .get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0)))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap
            .get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1)))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(2))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(2))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap
            .get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(2)))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(3))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(3))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap
            .get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(3)))
            .get(0).getExtension().getRepository());
  }

  @Test
  public void testExtensionsMetadataMapAnyVersion() {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    Map<StackGresExtensionIndexAnyVersion, List<StackGresExtensionMetadata>> extensionMetadataMap =
        ExtensionUtil.toExtensionsMetadataIndexAnyVersions(
            repository, extensionsMetadata);
    cluster.getSpec().setPostgresVersion(firstPgMajorVersion);
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(0))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(0))).size());
    Assertions.assertNotEquals(
        repository,
        extensionMetadataMap.get(new StackGresExtensionIndexAnyVersion(cluster, extensions.get(0)))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(1))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(1))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexAnyVersion(cluster, extensions.get(1)))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(2))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(2))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexAnyVersion(cluster, extensions.get(2)))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(3))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(3))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexAnyVersion(cluster, extensions.get(3)))
            .get(0).getExtension().getRepository());
    cluster.getSpec().setPostgresVersion(secondPgMajorVersion);
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(0))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(0))).size());
    Assertions.assertNotEquals(
        repository,
        extensionMetadataMap.get(new StackGresExtensionIndexAnyVersion(cluster, extensions.get(0)))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(1))));
    Assertions.assertEquals(2, extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(1))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexAnyVersion(cluster, extensions.get(1)))
            .get(0).getExtension().getRepository());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexAnyVersion(cluster, extensions.get(1)))
            .get(1).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(2))));
    Assertions.assertEquals(2, extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(2))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexAnyVersion(cluster, extensions.get(2)))
            .get(0).getExtension().getRepository());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexAnyVersion(cluster, extensions.get(2)))
            .get(1).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(3))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexAnyVersion(cluster, extensions.get(3))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexAnyVersion(cluster, extensions.get(3)))
            .get(0).getExtension().getRepository());
  }
}
