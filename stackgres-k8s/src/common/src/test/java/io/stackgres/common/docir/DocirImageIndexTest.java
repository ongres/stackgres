/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatusAddon;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocirImageIndexTest {

  private StackGresCluster cluster;

  private StackGresClusterInstalledExtension extension;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion("13.16");
    cluster.getStatus().setPostgresVersion("13.16");
    extension = new StackGresClusterInstalledExtension();
    extension.setName("dblink");
    extension.setRepository("https://sgcr.dev");
    extension.setVersion("13.16");
    extension.setPostgresVersion("13.16");
    extension.setBuild("10");
  }

  @Test
  void givenFinalVersionInSpec_shouldUseIt() {
    cluster.getStatus().setPostgresVersion("13.15");

    final DocirImageIndex index = DocirImageIndex.fromExtension(
        StackGresContextMock.CONTEXT, cluster, extension);

    assertEquals(13, index.getFlavor().getFlavor().getMajor());
    assertEquals(16, index.getFlavor().getFlavor().getMinor());
  }

  @Test
  void givenLatestVersionInSpec_shouldUseStatusVersion() {
    cluster.getSpec().getPostgres().setVersion("latest");

    final DocirImageIndex index = DocirImageIndex.fromExtension(
        StackGresContextMock.CONTEXT, cluster, extension);

    assertEquals(13, index.getFlavor().getFlavor().getMajor());
    assertEquals(16, index.getFlavor().getFlavor().getMinor());
  }

  @Test
  void givenMajorVersionInSpec_shouldUseStatusVersion() {
    cluster.getSpec().getPostgres().setVersion("13");

    final DocirImageIndex index = DocirImageIndex.fromExtension(
        StackGresContextMock.CONTEXT, cluster, extension);

    assertEquals(13, index.getFlavor().getFlavor().getMajor());
    assertEquals(16, index.getFlavor().getFlavor().getMinor());
  }

  @Test
  void imageNames_shouldDescribeTheComponentsAndVersions() {
    final StackGresClusterStatusAddon patroni = cluster.getStatus()
        .findAddon(DocirUtil.PATRONI_ADDON).orElseThrow();
    final String patroniVersion = patroni.getVersion() + "-" + patroni.getRevision();
    final StackGresClusterStatusAddon pgbouncer = cluster.getStatus()
        .findAddon(DocirUtil.PGBOUNCER_ADDON).orElseThrow();

    assertEquals(
        "patroni-" + patroniVersion + "-postgres-13.16",
        DocirImageIndex.fromCluster(StackGresContextMock.CONTEXT, cluster).getImageName());
    final DocirImageIndex postgresUtil =
        DocirImageIndex.fromClusterForPostgresUtil(StackGresContextMock.CONTEXT, cluster);
    assertEquals("postgres-util-" + patroniVersion + "-postgres-13.16", postgresUtil.getImageName());
    assertEquals(DocirImageIndex.fromCluster(StackGresContextMock.CONTEXT, cluster), postgresUtil);
    assertEquals(
        "pgbouncer-" + pgbouncer.getVersion() + "-" + pgbouncer.getRevision(),
        DocirImageIndex.fromAddon(StackGresContextMock.CONTEXT, cluster, DocirUtil.PGBOUNCER_ADDON)
        .getImageName());
    assertEquals(
        "postgres-13.16-dblink-13.16-10",
        DocirImageIndex.fromExtension(StackGresContextMock.CONTEXT, cluster, extension)
        .getImageName());
    final StackGresCluster oldCluster = Fixtures.cluster().loadDefault().get();
    oldCluster.getSpec().getPostgres().setVersion("13.15");
    oldCluster.getStatus().setPostgresVersion("13.15");
    assertEquals(
        "patroni-" + patroniVersion + "-postgres-13.15-to-13.16",
        DocirImageIndex.fromClusters(StackGresContextMock.CONTEXT, cluster, oldCluster)
        .getImageName());
  }

  @Test
  void imageUrlRequest_shouldCarryTheImageNameAsName() throws Exception {
    final DocirImageIndex index = DocirImageIndex.fromAddon(
        StackGresContextMock.CONTEXT, cluster, DocirUtil.PGBOUNCER_ADDON);

    final String json = JsonUtil.jsonMapper().writeValueAsString(
        StackGresContextMock.CONTEXT.getMetadataManager().toImageUrlRequest(index));

    assertTrue(json.contains("\"name\":\"" + index.getImageName() + "\""), json);
    assertFalse(json.contains("imageName"), json);
  }

  @Test
  void imageNameOf_shouldProduceValidRepositoryNames() {
    assertEquals("patroni-4.1.0-babelfishpg-16.4",
        DocirImageIndex.imageNameOf("patroni", "4.1.0", null, "BabelfishPG", "16.4"));
    assertEquals("pg_audit-1.7-1-7", DocirImageIndex.imageNameOf("pg_audit", "1.7+1", "7"));
    assertEquals("", DocirImageIndex.imageNameOf(null, ""));
  }

  @Test
  void baseOf_shouldReadThePinsOfTheStatus() {
    final DocirBase base = DocirImageIndex.baseOf(cluster);

    assertEquals(cluster.getStatus().getBase(), base.getName());
    assertEquals(cluster.getStatus().getBaseVersion(), base.getMajor() + "." + base.getMinor());
    assertEquals(cluster.getStatus().getBaseRevision(), base.getRevision());
    assertEquals(cluster.getStatus().getRepository(), base.getRepository());
  }

}
