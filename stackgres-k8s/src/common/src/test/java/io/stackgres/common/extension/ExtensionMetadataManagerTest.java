/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;

import io.stackgres.common.WebClientFactory;
import io.stackgres.common.WebClientFactory.WebClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.fixture.Fixtures;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtensionMetadataManagerTest {

  private static final URI REPOSITORY =
      URI.create("https://extensions.stackgres.io/postgres/repository");

  private static final String EXTENSION_NAME = "pg_ext";

  private static final String EXTENSION_VERSION = "1.0.0";

  @Mock
  private WebClientFactory webClientFactory;

  @Mock
  private WebClient webClient;

  private ExtensionMetadataManager extensionMetadataManager;

  @BeforeEach
  void setUp() throws Exception {
    extensionMetadataManager = new ExtensionMetadataManager(webClientFactory,
        List.of(REPOSITORY), () -> Map.of()) {};
    when(webClientFactory.create(any(), any())).thenReturn(webClient);
  }

  @ParameterizedTest
  @CsvSource({
      "17.8, 6.57",
      "17.9, 6.57",
      "17.10, 6.58",
      "17.11, 6.58",
  })
  void getExtensionsSameMajorBuild_shouldReturnTheBuildForThePostgresMinor(
      String postgresVersion, String expectedBuild) {
    when(webClient.getJson(ExtensionUtil.getIndexUri(REPOSITORY), StackGresExtensions.class))
        .thenReturn(getExtensions());

    List<StackGresExtensionMetadata> extensions =
        extensionMetadataManager.getExtensionsSameMajorBuild(
            getCluster(postgresVersion), getClusterExtension(), false);

    assertEquals(List.of(expectedBuild), extensions.stream()
        .map(StackGresExtensionMetadata::getTarget)
        .map(StackGresExtensionVersionTarget::getBuild)
        .toList());
  }

  @ParameterizedTest
  @CsvSource({
      "17.8, 6.57",
      "17.9, 6.57",
      "17.10, 6.58",
      "17.11, 6.58",
  })
  void getExtensionsAnyVersion_shouldReturnTheBuildForThePostgresMinor(
      String postgresVersion, String expectedBuild) {
    when(webClient.getJson(ExtensionUtil.getIndexUri(REPOSITORY), StackGresExtensions.class))
        .thenReturn(getExtensions());

    List<StackGresExtensionMetadata> extensions =
        extensionMetadataManager.getExtensionsAnyVersion(
            getCluster(postgresVersion), getClusterExtension(), false);

    assertEquals(List.of(expectedBuild), extensions.stream()
        .map(StackGresExtensionMetadata::getTarget)
        .map(StackGresExtensionVersionTarget::getBuild)
        .toList());
  }

  @ParameterizedTest
  @CsvSource({
      "17.8, true",
      "17.9, true",
      "17.10, false",
      "17.11, false",
  })
  void getExtensionsSameMajorBuild_shouldNotReturnABuildForAnOlderPostgresMinor(
      String postgresVersion, boolean expectBuild) {
    when(webClient.getJson(ExtensionUtil.getIndexUri(REPOSITORY), StackGresExtensions.class))
        .thenReturn(getExtensionsWithoutNewerBuild());

    List<StackGresExtensionMetadata> extensions =
        extensionMetadataManager.getExtensionsSameMajorBuild(
            getCluster(postgresVersion), getClusterExtension(), false);

    assertEquals(expectBuild ? List.of("6.57") : List.of(), extensions.stream()
        .map(StackGresExtensionMetadata::getTarget)
        .map(StackGresExtensionVersionTarget::getBuild)
        .toList());
  }

  @ParameterizedTest
  @CsvSource({
      "17.8, true",
      "17.9, true",
      "17.10, false",
      "17.11, false",
  })
  void getExtensionsAnyVersion_shouldNotReturnABuildForAnOlderPostgresMinor(
      String postgresVersion, boolean expectBuild) {
    when(webClient.getJson(ExtensionUtil.getIndexUri(REPOSITORY), StackGresExtensions.class))
        .thenReturn(getExtensionsWithoutNewerBuild());

    List<StackGresExtensionMetadata> extensions =
        extensionMetadataManager.getExtensionsAnyVersion(
            getCluster(postgresVersion), getClusterExtension(), false);

    assertEquals(expectBuild ? List.of("6.57") : List.of(), extensions.stream()
        .map(StackGresExtensionMetadata::getTarget)
        .map(StackGresExtensionVersionTarget::getBuild)
        .toList());
  }

  @Test
  void getExtensions_shouldReadTheIndexV3() {
    when(webClient.getJson(ExtensionUtil.getIndexUri(REPOSITORY), StackGresExtensions.class))
        .thenReturn(getExtensions());

    assertEquals(2, extensionMetadataManager.getExtensions().size());

    assertEquals(URI.create(REPOSITORY + "/v3/index.json"),
        ExtensionUtil.getIndexUri(REPOSITORY));
  }

  @Test
  void getExtensions_shouldNotReadTheIndexV2WhenTheIndexV3IsNotFound() {
    when(webClient.getJson(ExtensionUtil.getIndexUri(REPOSITORY), StackGresExtensions.class))
        .thenThrow(new NotFoundException());

    assertThrows(RuntimeException.class, () -> extensionMetadataManager.getExtensions());

    verify(webClient, times(1)).getJson(any(), any());
  }

  private StackGresCluster getCluster(String postgresVersion) {
    StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(postgresVersion);
    return cluster;
  }

  private StackGresClusterExtension getClusterExtension() {
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName(EXTENSION_NAME);
    extension.setVersion(EXTENSION_VERSION);
    return extension;
  }

  private StackGresExtensions getExtensions() {
    return getExtensionsAvailableFor(
        getPreviousBuild(),
        // Built for the Postgres minors released with an ABI change
        new StackGresExtensionVersionTargetBuilder()
        .withPostgresVersion("17")
        .withPostgresFromVersion("17.10")
        .withBuild("6.58")
        .build());
  }

  private StackGresExtensions getExtensionsWithoutNewerBuild() {
    return getExtensionsAvailableFor(getPreviousBuild());
  }

  private StackGresExtensionVersionTarget getPreviousBuild() {
    // Does not run on the Postgres minors released with an ABI change
    return new StackGresExtensionVersionTargetBuilder()
        .withPostgresVersion("17")
        .withPostgresToVersion("17.9")
        .withBuild("6.57")
        .build();
  }

  private StackGresExtensions getExtensionsAvailableFor(
      StackGresExtensionVersionTarget... availableFor) {
    StackGresExtensions extensions = Fixtures.extensionMetadata().loadDefault().get();
    extensions.setExtensions(List.of(new StackGresExtensionBuilder()
        .withName(EXTENSION_NAME)
        .withPublisher("com.ongres")
        .withChannels(Map.of(ExtensionUtil.DEFAULT_CHANNEL, EXTENSION_VERSION))
        .withVersions(new StackGresExtensionVersionBuilder()
            .withVersion(EXTENSION_VERSION)
            .withAvailableFor(availableFor)
            .build())
        .build()));
    return extensions;
  }

}
