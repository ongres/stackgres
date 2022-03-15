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
  @ValueSource(strings = {"16", "17", "18"})
  void testImageName17(String minor) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder().withMinor(minor).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.findImageName("1.17"))
        .orElseThrow();
    String imageName = KubectlUtil.fromClient(mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

  @ParameterizedTest
  @ValueSource(strings = {"19", "20", "21"})
  void testImageName20(String minor) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder().withMinor(minor).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.findImageName("1.20"))
        .orElseThrow();
    String imageName = KubectlUtil.fromClient(mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

  @ParameterizedTest
  @ValueSource(strings = {"22", "23", "24"})
  void testImageName23(String minor) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder().withMinor(minor).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.findImageName("1.23"))
        .orElseThrow();
    String imageName = KubectlUtil.fromClient(mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

  @ParameterizedTest
  @ValueSource(strings = {"12", "15", "99"})
  void testImageNameUnknow(String minor) {
    KubernetesClient mockClient = Mockito.mock(KubernetesClient.class);

    var versionInfo = new VersionInfo.Builder().withMinor(minor).build();
    Mockito.when(mockClient.getKubernetesVersion()).thenReturn(versionInfo);

    // Always return the latest image name since older versions
    // are unsupported anyway. So expect "newer" versions of K8s instead.
    String expected = StackGresComponent.KUBECTL.get(StackGresVersion.LATEST)
        .map(c -> c.findLatestImageName())
        .orElseThrow();
    String imageName = KubectlUtil.fromClient(mockClient)
        .getImageName(StackGresVersion.LATEST);

    assertEquals(expected, imageName);
  }

}
