/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class KubectlUtilTest {

  @ParameterizedTest
  @ValueSource(strings = {"v1.16.10", "v1.17.11", "v1.18.1"})
  void testImageName17(String version) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder()
        .withGitVersion(version).withMinor(version.split("\\.")[1]).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.getImageName("1.17"))
        .orElseThrow();
    String imageName = new KubectlUtil(mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

  @ParameterizedTest
  @ValueSource(strings = {"v1.19", "v1.20.2", "v1.21.7"})
  void testImageName20(String version) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder()
        .withGitVersion(version).withMinor(version.split("\\.")[1]).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.getImageName("1.20"))
        .orElseThrow();
    String imageName = new KubectlUtil(mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

  @ParameterizedTest
  @ValueSource(strings = {"v1.22.9", "v1.23.3", "v1.24.0"})
  void testImageName23(String version) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder()
        .withGitVersion(version).withMinor(version.split("\\.")[1]).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.getImageName("1.23"))
        .orElseThrow();
    String imageName = new KubectlUtil(mockClient)
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
        .map(c -> c.getLatestImageName())
        .orElseThrow();
    String imageName = new KubectlUtil(mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

}
