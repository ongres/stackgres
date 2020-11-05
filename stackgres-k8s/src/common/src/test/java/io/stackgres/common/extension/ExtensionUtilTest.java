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
import org.junit.jupiter.api.Test;

public class ExtensionUtilTest {

  @Test
  public void testExtensionsMetadataMap() {
    StackGresExtensions extensionsMetadata = JsonUtil
        .readFromJson("extension_metadata/index.json",
            StackGresExtensions.class);
    final String pgVersion = StackGresComponent.POSTGRESQL.getOrderedVersions()
        .findAny().get();
    final String firstPgMajorVersion = StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
        .get(0).get();
    final String secondPgMajorVersion = StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
        .get(1).get();
    final String build = StackGresComponent.POSTGRESQL.findBuildVersion(pgVersion);
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
    List<StackGresClusterExtension> extensions = JsonUtil
        .readListFromJson("extension_metadata/extensions.json",
            StackGresClusterExtension.class);
    final URI repository = URI.create("https://extensions.stackgres.io/postgres/repository?skipHostVerification=true");
    StackGresClusterInstalledExtension installedExtension0 = new StackGresClusterInstalledExtension();
    installedExtension0.setName(extensions.get(0).getName());
    installedExtension0.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension0.setRepository(repository.toASCIIString());
    installedExtension0.setVersion(extensions.get(0).getVersion());
    installedExtension0.setPostgresVersion(firstPgMajorVersion);
    installedExtension0.setBuild(build);
    StackGresClusterInstalledExtension installedExtension1 = new StackGresClusterInstalledExtension();
    installedExtension1.setName(extensions.get(1).getName());
    installedExtension1.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension1.setRepository(repository.toASCIIString());
    installedExtension1.setVersion(extensionsMetadata.getExtensions().stream()
        .filter(e -> e.getName().equals(extensions.get(1).getName())).findAny().get()
        .getVersions().get(0).getVersion());
    installedExtension1.setPostgresVersion(firstPgMajorVersion);
    installedExtension1.setBuild(build);
    StackGresClusterInstalledExtension installedExtension2 = new StackGresClusterInstalledExtension();
    installedExtension2.setName(extensions.get(2).getName());
    installedExtension2.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension2.setRepository(repository.toASCIIString());
    installedExtension2.setVersion(extensions.get(2).getVersion());
    installedExtension2.setPostgresVersion(firstPgMajorVersion);
    installedExtension2.setBuild(build);
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
    installedExtension0.setPostgresVersion(secondPgMajorVersion);
    installedExtension1.setPostgresVersion(secondPgMajorVersion);
    installedExtension2.setPostgresVersion(secondPgMajorVersion);
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
  }

  @Test
  public void testExtensionsMetadataMapSameMajorBuild() {
    StackGresExtensions extensionsMetadata = JsonUtil
        .readFromJson("extension_metadata/index.json",
            StackGresExtensions.class);
    final String pgVersion = StackGresComponent.POSTGRESQL.getOrderedVersions()
        .findAny().get();
    final String firstPgMajorVersion = StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
        .get(0).get();
    final String secondPgMajorVersion = StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
        .get(1).get();
    final String build = StackGresComponent.POSTGRESQL.findBuildVersion(pgVersion);
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
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    List<StackGresClusterExtension> extensions = JsonUtil
        .readListFromJson("extension_metadata/extensions.json",
            StackGresClusterExtension.class);
    final URI repository = URI.create("https://extensions.stackgres.io/postgres/repository?skipHostVerification=true");
    Map<StackGresExtensionIndexSameMajorBuild, List<StackGresExtensionMetadata>> extensionMetadataMap =
        ExtensionUtil.toExtensionsMetadataIndexSameMajorBuilds(
            repository, extensionsMetadata);
    cluster.getSpec().setPostgresVersion(firstPgMajorVersion);
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0))).size());
    Assertions.assertNotEquals(
        repository,
        extensionMetadataMap.get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0)))
        .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1)))
        .get(0).getExtension().getRepository());
    Assertions.assertNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(2))));
    cluster.getSpec().setPostgresVersion(secondPgMajorVersion);
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0))).size());
    Assertions.assertNotEquals(
        repository,
        extensionMetadataMap.get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(0)))
        .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(1)))
        .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(2))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(2))).size());
    Assertions.assertEquals(
        repository.toASCIIString(),
        extensionMetadataMap.get(new StackGresExtensionIndexSameMajorBuild(cluster, extensions.get(2)))
        .get(0).getExtension().getRepository());
  }

  @Test
  public void testExtensionsMetadataMapAnyVersion() {
    StackGresExtensions extensionsMetadata = JsonUtil
        .readFromJson("extension_metadata/index.json",
            StackGresExtensions.class);
    final String pgVersion = StackGresComponent.POSTGRESQL.getOrderedVersions()
        .findAny().get();
    final String firstPgMajorVersion = StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
        .get(0).get();
    final String secondPgMajorVersion = StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
        .get(1).get();
    final String build = StackGresComponent.POSTGRESQL.findBuildVersion(pgVersion);
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
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    List<StackGresClusterExtension> extensions = JsonUtil
        .readListFromJson("extension_metadata/extensions.json",
            StackGresClusterExtension.class);
    final URI repository = URI.create("https://extensions.stackgres.io/postgres/repository?skipHostVerification=true");
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
  }
}
