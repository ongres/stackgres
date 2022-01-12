/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExtensionUtilTest {

  static final URI REPOSITORY =
      URI.create("https://extensions.stackgres.io/postgres/repository");
  static final String PG_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedVersions()
      .findAny().get();
  static final String FIRST_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedMajorVersions()
      .get(0).get();
  static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedMajorVersions()
      .get(1).get();
  static final String SECOND_PG_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().findVersion(SECOND_PG_MAJOR_VERSION);
  static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().findBuildVersion(PG_VERSION);
  static final String BUILD_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().findBuildMajorVersion(PG_VERSION);

  StackGresExtensions extensionsMetadata;

  Map<String, StackGresClusterExtension> extensions;

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
    extensions = Seq.seq(JsonUtil
        .readListFromJson("extension_metadata/extensions.json",
            StackGresClusterExtension.class))
        .collect(ImmutableMap.toImmutableMap(
            e -> e.getName() + Optional.ofNullable(e.getVersion())
              .map(v -> "-" + v).orElse(""),
            Function.identity()));
  }

  @Test
  public void testExtensionsMetadataMap() {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);
    StackGresClusterInstalledExtension installedExtension0 =
        new StackGresClusterInstalledExtension();
    installedExtension0.setName(extensions.get("pgsodium-1.1.0").getName());
    installedExtension0.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension0.setRepository(REPOSITORY.toString());
    installedExtension0.setVersion(extensions.get("pgsodium-1.1.0").getVersion());
    installedExtension0.setPostgresVersion(FIRST_PG_MAJOR_VERSION);
    installedExtension0.setBuild(BUILD_VERSION);
    StackGresClusterInstalledExtension installedExtension1 =
        new StackGresClusterInstalledExtension();
    installedExtension1.setName(extensions.get("mysqlcompat").getName());
    installedExtension1.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension1.setRepository(REPOSITORY.toString());
    installedExtension1.setVersion(extensionsMetadata.getExtensions().stream()
        .filter(e -> e.getName().equals(extensions.get("mysqlcompat").getName())).findAny().get()
        .getVersions().get(0).getVersion());
    installedExtension1.setPostgresVersion(FIRST_PG_MAJOR_VERSION);
    installedExtension1.setBuild(BUILD_VERSION);
    StackGresClusterInstalledExtension installedExtension2 =
        new StackGresClusterInstalledExtension();
    installedExtension2.setName(extensions.get("mysqlcompat-0.0.6").getName());
    installedExtension2.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension2.setRepository(REPOSITORY.toString());
    installedExtension2.setVersion(extensions.get("mysqlcompat-0.0.6").getVersion());
    installedExtension2.setPostgresVersion(FIRST_PG_MAJOR_VERSION);
    installedExtension2.setBuild(BUILD_VERSION);
    StackGresClusterInstalledExtension installedExtension3 =
        new StackGresClusterInstalledExtension();
    installedExtension3.setName(extensions.get("plpgsql").getName());
    installedExtension3.setPublisher(extensionsMetadata.getPublishers().get(0).getId());
    installedExtension3.setRepository(REPOSITORY.toString());
    installedExtension3.setVersion(extensionsMetadata.getExtensions().stream()
        .filter(e -> e.getName().equals(extensions.get("plpgsql").getName())).findAny().get()
        .getVersions().get(0).getVersion());
    installedExtension3.setPostgresVersion(PG_VERSION);
    installedExtension3.setBuild(BUILD_VERSION);
    Map<StackGresExtensionIndex, StackGresExtensionMetadata> extensionMetadataMap =
        ExtensionUtil.toExtensionsMetadataIndex(
            REPOSITORY, extensionsMetadata);
    Assertions.assertNotNull(extensionMetadataMap.get(StackGresExtensionIndex
        .fromClusterInstalledExtension(cluster, installedExtension0, false)));
    Assertions.assertNotEquals(
        REPOSITORY,
        extensionMetadataMap.get(StackGresExtensionIndex
            .fromClusterInstalledExtension(cluster, installedExtension0, false))
            .getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(StackGresExtensionIndex
        .fromClusterInstalledExtension(cluster, installedExtension1, false)));
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(StackGresExtensionIndex
            .fromClusterInstalledExtension(cluster, installedExtension1, false))
        .getExtension().getRepository());
    Assertions.assertNull(extensionMetadataMap.get(StackGresExtensionIndex
        .fromClusterInstalledExtension(cluster, installedExtension2, false)));
    Assertions.assertNotNull(extensionMetadataMap.get(StackGresExtensionIndex
        .fromClusterInstalledExtension(cluster, installedExtension3, false)));
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(StackGresExtensionIndex
            .fromClusterInstalledExtension(cluster, installedExtension3, false))
            .getExtension().getRepository());
    installedExtension0.setPostgresVersion(SECOND_PG_MAJOR_VERSION);
    installedExtension1.setPostgresVersion(SECOND_PG_MAJOR_VERSION);
    installedExtension2.setPostgresVersion(SECOND_PG_MAJOR_VERSION);
    installedExtension3.setPostgresVersion(SECOND_PG_VERSION);
    Assertions.assertNotNull(extensionMetadataMap.get(StackGresExtensionIndex
        .fromClusterInstalledExtension(cluster, installedExtension0, false)));
    Assertions.assertNotEquals(
        REPOSITORY,
        extensionMetadataMap.get(StackGresExtensionIndex
            .fromClusterInstalledExtension(cluster, installedExtension0, false))
        .getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(StackGresExtensionIndex
        .fromClusterInstalledExtension(cluster, installedExtension1, false)));
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(StackGresExtensionIndex
            .fromClusterInstalledExtension(cluster, installedExtension1, false))
        .getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(StackGresExtensionIndex
        .fromClusterInstalledExtension(cluster, installedExtension2, false)));
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(StackGresExtensionIndex
            .fromClusterInstalledExtension(cluster, installedExtension2, false))
        .getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(StackGresExtensionIndex
        .fromClusterInstalledExtension(cluster, installedExtension3, false)));
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(StackGresExtensionIndex
            .fromClusterInstalledExtension(cluster, installedExtension3, false))
        .getExtension().getRepository());
  }

  @Test
  public void testExtensionsMetadataMapSameMajorBuild() {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    var extensionMetadataMap = ExtensionUtil.toExtensionsMetadataIndexSameMajorBuilds(
        REPOSITORY, extensionsMetadata);
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("pgsodium-1.1.0"))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("pgsodium-1.1.0"))).size());
    Assertions.assertNotEquals(
        REPOSITORY,
        extensionMetadataMap
            .get(indexSameMajorBuild(cluster, extensions.get("pgsodium-1.1.0")))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("mysqlcompat"))));
    Assertions.assertEquals(2, extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("mysqlcompat"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap
            .get(indexSameMajorBuild(cluster, extensions.get("mysqlcompat")))
            .get(0).getExtension().getRepository());
    Assertions.assertNull(extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("mysqlcompat-0.0.6"))));
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("plpgsql"))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("plpgsql"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap
            .get(indexSameMajorBuild(cluster, extensions.get("plpgsql")))
            .get(0).getExtension().getRepository());
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("pgsodium-1.1.0"))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("pgsodium-1.1.0"))).size());
    Assertions.assertNotEquals(
        REPOSITORY,
        extensionMetadataMap
            .get(indexSameMajorBuild(cluster, extensions.get("pgsodium-1.1.0")))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("mysqlcompat"))));
    Assertions.assertEquals(2, extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("mysqlcompat"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap
            .get(indexSameMajorBuild(cluster, extensions.get("mysqlcompat")))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("mysqlcompat-0.0.6"))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("mysqlcompat-0.0.6"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap
            .get(indexSameMajorBuild(cluster, extensions.get("mysqlcompat-0.0.6")))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("plpgsql"))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        indexSameMajorBuild(cluster, extensions.get("plpgsql"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap
            .get(indexSameMajorBuild(cluster, extensions.get("plpgsql")))
            .get(0).getExtension().getRepository());
  }

  @Test
  public void testExtensionsMetadataMapAnyVersion() {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    Map<StackGresExtensionIndexAnyVersion, List<StackGresExtensionMetadata>> extensionMetadataMap =
        ExtensionUtil.toExtensionsMetadataIndexAnyVersions(
            REPOSITORY, extensionsMetadata);
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);
    Assertions.assertNotNull(extensionMetadataMap.get(
        StackGresExtensionIndexAnyVersion.fromClusterExtension(
            cluster, extensions.get("pgsodium-1.1.0"), false)));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        StackGresExtensionIndexAnyVersion.fromClusterExtension(
            cluster, extensions.get("pgsodium-1.1.0"), false)).size());
    Assertions.assertNotEquals(
        REPOSITORY,
        extensionMetadataMap.get(indexAnyVersion(cluster, extensions.get("pgsodium-1.1.0")))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("mysqlcompat"))));
    Assertions.assertEquals(2, extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("mysqlcompat"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(indexAnyVersion(cluster, extensions.get("mysqlcompat")))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("mysqlcompat-0.0.6"))));
    Assertions.assertEquals(2, extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("mysqlcompat-0.0.6"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(indexAnyVersion(cluster, extensions.get("mysqlcompat-0.0.6")))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("plpgsql"))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("plpgsql"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(indexAnyVersion(cluster, extensions.get("plpgsql")))
            .get(0).getExtension().getRepository());
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("pgsodium-1.1.0"))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("pgsodium-1.1.0"))).size());
    Assertions.assertNotEquals(
        REPOSITORY,
        extensionMetadataMap.get(indexAnyVersion(cluster,
            extensions.get("pgsodium-1.1.0")))
            .get(0).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("mysqlcompat"))));
    Assertions.assertEquals(3, extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("mysqlcompat"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(indexAnyVersion(cluster, extensions.get("mysqlcompat")))
            .get(0).getExtension().getRepository());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(indexAnyVersion(cluster, extensions.get("mysqlcompat")))
            .get(1).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("mysqlcompat-0.0.6"))));
    Assertions.assertEquals(3, extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("mysqlcompat-0.0.6"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(indexAnyVersion(cluster, extensions.get("mysqlcompat-0.0.6")))
            .get(0).getExtension().getRepository());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(indexAnyVersion(cluster, extensions.get("mysqlcompat-0.0.6")))
            .get(1).getExtension().getRepository());
    Assertions.assertNotNull(extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("plpgsql"))));
    Assertions.assertEquals(1, extensionMetadataMap.get(
        indexAnyVersion(cluster, extensions.get("plpgsql"))).size());
    Assertions.assertEquals(
        REPOSITORY.toString(),
        extensionMetadataMap.get(indexAnyVersion(cluster, extensions.get("plpgsql")))
            .get(0).getExtension().getRepository());
  }

  StackGresExtensionIndexSameMajorBuild indexSameMajorBuild(StackGresCluster cluster,
      StackGresClusterExtension extension) {
    return StackGresExtensionIndexSameMajorBuild.fromClusterExtension(cluster, extension, false);
  }

  StackGresExtensionIndexAnyVersion indexAnyVersion(StackGresCluster cluster,
      StackGresClusterExtension extension) {
    return StackGresExtensionIndexAnyVersion.fromClusterExtension(cluster, extension, false);
  }
}
