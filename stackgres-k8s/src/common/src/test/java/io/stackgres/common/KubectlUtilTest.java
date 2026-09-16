/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.docir.StackGresContextMock.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.net.URI;
import java.util.List;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.docir.DocirAddonVersion;
import io.stackgres.common.docir.DocirBase;
import io.stackgres.common.docir.DocirMetadataManager;
import io.stackgres.common.docir.DocirUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class KubectlUtilTest {

  private static final URI REPOSITORY_URI = URI.create("https://sgcr.dev");

  @ParameterizedTest
  @ValueSource(strings = {"v1.18.10", "v1.19.11", "v1.20.1"})
  void testImageName17(String version) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder()
        .withGitVersion(version).withMinor(version.split("\\.")[1]).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.getImageName(CONTEXT, "1.19"))
        .orElseThrow();
    String imageName = new KubectlUtil(CONTEXT, mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

  @ParameterizedTest
  @ValueSource(strings = {"v1.21", "v1.22.2", "v1.23.7"})
  void testImageName20(String version) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder()
        .withGitVersion(version).withMinor(version.split("\\.")[1]).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.getImageName(CONTEXT, "1.22"))
        .orElseThrow();
    String imageName = new KubectlUtil(CONTEXT, mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

  @ParameterizedTest
  @ValueSource(strings = {"v1.24.9", "v1.25.3", "v1.26.0"})
  void testImageName23(String version) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder()
        .withGitVersion(version).withMinor(version.split("\\.")[1]).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.getImageName(CONTEXT, "1.25"))
        .orElseThrow();
    String imageName = new KubectlUtil(CONTEXT, mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

  @ParameterizedTest
  @ValueSource(strings = {"v1.12.15", "v1.14.0", "v1.15.15", "v1.99.99"})
  void testImageNameUnknow(String version) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder()
        .withGitVersion(version).withMinor(version.split("\\.")[1]).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    // Always return the latest image name since older versions
    // are unsupported anyway. So expect "newer" versions of K8s instead.
    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.getLatestImageName(CONTEXT))
        .orElseThrow();
    String imageName = new KubectlUtil(CONTEXT, mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

  @Test
  void testAddonVersionNearerToKubernetes() {
    assertEquals("1.30.4", findAddonVersion("v1.28.9", "1.37.0", "1.33.1", "1.30.4"));
    assertEquals("1.33.1", findAddonVersion("v1.32.1", "1.37.0", "1.33.1", "1.30.4"));
    assertEquals("1.37.0", findAddonVersion("v1.36.0", "1.37.0", "1.33.1", "1.30.4"));
  }

  @Test
  void testAddonVersionNearerToKubernetesAtTheSameDistance() {
    assertEquals("1.32.0", findAddonVersion("v1.31.4", "1.32.0", "1.30.4"));
  }

  @Test
  void testAddonVersionWithUnknownKubernetesVersion() {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);
    Mockito.when(mockClient.getKubernetesVersion()).thenThrow(new RuntimeException("test"));

    assertEquals("1.37.0",
        findAddonVersion(mockClient, "1.37.0", "1.33.1", "1.30.4"));
  }

  private String findAddonVersion(String kubernetesVersion, String... versions) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);
    var versionInfo = new VersionInfo.Builder()
        .withGitVersion(kubernetesVersion)
        .withMinor(kubernetesVersion.split("\\.")[1]).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);
    return findAddonVersion(mockClient, versions);
  }

  /**
   * The versions are returned by the metadata manager from the latest to the oldest, as
   * {@link DocirMetadataManager#getAddonVersions(URI, String, DocirBase, String, String)} does.
   */
  private String findAddonVersion(KubernetesClient mockClient, String... versions) {
    StackGresContext mockContext = Mockito.mock(StackGresContext.class);
    DocirMetadataManager mockMetadataManager = Mockito.mock(DocirMetadataManager.class);
    DocirBase base = Mockito.mock(DocirBase.class);
    Mockito.when(mockContext.getMetadataManager()).thenReturn(mockMetadataManager);
    Mockito.when(mockMetadataManager.getAddonVersions(
        any(), eq(DocirUtil.KUBECTL_ADDON), any(), any(), any()))
        .thenReturn(List.of(versions).stream()
            .map(KubectlUtilTest::getAddonVersion)
            .toList());

    return new KubectlUtil(mockContext, mockClient)
        .findAddonVersion(REPOSITORY_URI, base, DocirUtil.DEFAULT_OS, DocirUtil.DEFAULT_ARCH)
        .map(DocirAddonVersion::getVersion)
        .orElseThrow();
  }

  private static DocirAddonVersion getAddonVersion(String version) {
    DocirAddonVersion addonVersion = new DocirAddonVersion();
    addonVersion.setVersion(version);
    addonVersion.setRevision("1");
    return addonVersion;
  }

}
