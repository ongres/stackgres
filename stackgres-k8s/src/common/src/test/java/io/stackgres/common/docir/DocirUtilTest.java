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

  @Test
  void flavorsMetadataIndex_shouldUseTheLatestRevisionOfTheBaseImage() {
    final List<DocirBase> bases = List.of(
        getBase("8804"), getBase("8856"), getBase("8818"));

    final var flavorsMetadataIndex = DocirUtil.toFlavorsMetadataIndex(
        List.of(getFlavor("10119")), bases, getAddons());

    assertEquals(1, flavorsMetadataIndex.size());
    final DocirFlavorMetadata flavorMetadata =
        flavorsMetadataIndex.values().iterator().next();
    assertEquals("8856", flavorMetadata.getBaseRevision());
    assertEquals("8856", flavorMetadata.getBase().getRevision());
  }

  @Test
  void flavorsMetadataIndex_shouldBeEmptyWhenTheBaseImageIsNotAvailable() {
    final DocirBase otherBase = getBase("8856");
    otherBase.setMinor(4);

    final var flavorsMetadataIndex = DocirUtil.toFlavorsMetadataIndex(
        List.of(getFlavor("10119")), List.of(otherBase), getAddons());

    assertTrue(flavorsMetadataIndex.isEmpty());
  }

  private static DocirFlavor getFlavor(String revision) {
    final DocirFlavor flavor = new DocirFlavor();
    flavor.setRepository(REPOSITORY.toString());
    flavor.setName(DocirUtil.POSTGRES_FLAVOR);
    flavor.setMajor(17);
    flavor.setMinor(11);
    flavor.setRevision(revision);
    flavor.setBaseName("debian");
    flavor.setBaseMajor(13);
    flavor.setBaseMinor(3);
    return flavor;
  }

  private static DocirBase getBase(String revision) {
    final DocirBase base = new DocirBase();
    base.setRepository(REPOSITORY.toString());
    base.setName("debian");
    base.setMajor(13);
    base.setMinor(3);
    base.setRevision(revision);
    return base;
  }

  private static List<DocirAddon> getAddons() {
    return List.of(
        getAddon(DocirUtil.PATRONI_ADDON, "4.1.0", "9216"),
        getAddon(DocirUtil.WALG_ADDON, "3.0.9", "9239"),
        getAddon(DocirUtil.HDRHISTOGRAM_ADDON, "0.10.7", "9209"));
  }

  private static DocirAddon getAddon(String name, String version, String revision) {
    final DocirAddonVersion addonVersion = new DocirAddonVersion();
    addonVersion.setVersion(version);
    addonVersion.setRevision(revision);
    addonVersion.setBaseName("debian");
    addonVersion.setBaseMajor(13);
    addonVersion.setBaseMinor(3);
    final DocirAddon addon = new DocirAddon();
    addon.setRepository(REPOSITORY.toString());
    addon.setName(name);
    addon.setVersions(List.of(addonVersion));
    return addon;
  }

}
