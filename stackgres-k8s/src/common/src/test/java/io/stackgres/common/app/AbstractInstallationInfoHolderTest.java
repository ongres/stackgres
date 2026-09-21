/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.StackGresProperty;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AbstractInstallationInfoHolderTest {

  private static final String EXTRA_METADATA_PROPERTY =
      StackGresProperty.INSTALLATION_EXTRA_METADATA.getPropertyName();

  private static final String INSTALLATION_ID = "YED9Ug";

  private static class InstallationInfoHolder extends AbstractInstallationInfoHolder {
    InstallationInfoHolder(KubernetesClient client) {
      super(client);
    }

    @Override
    protected String retrieveInstallationId() {
      return INSTALLATION_ID;
    }
  }

  private static KubernetesClient client() {
    VersionInfo versionInfo = mock(VersionInfo.class);
    when(versionInfo.getGitVersion()).thenReturn("v1.33.6+rke2r1");
    KubernetesClient client = mock(KubernetesClient.class);
    when(client.getKubernetesVersion()).thenReturn(versionInfo);
    return client;
  }

  private static String userAgent() {
    var headerEntry = new InstallationInfoHolder(client()).getUserAgentHeaderEntry();
    assertEquals(HttpHeaders.USER_AGENT, headerEntry.getKey());
    return headerEntry.getValue();
  }

  @AfterEach
  void clearExtraMetadata() {
    System.clearProperty(EXTRA_METADATA_PROPERTY);
  }

  @Test
  void withoutExtraMetadataTheInstallationIdIsTheLastEntry() {
    assertTrue(userAgent().endsWith("; " + INSTALLATION_ID + ")"), userAgent());
    assertFalse(userAgent().contains("Env"), userAgent());
  }

  @Test
  void withBlankExtraMetadataTheInstallationIdIsTheLastEntry() {
    System.setProperty(EXTRA_METADATA_PROPERTY, "   ");
    assertTrue(userAgent().endsWith("; " + INSTALLATION_ID + ")"), userAgent());
  }

  @Test
  void extraMetadataIsAddedJustBeforeTheInstallationId() {
    System.setProperty(EXTRA_METADATA_PROPERTY, "Env stackgres-ci");
    assertTrue(userAgent().endsWith("; Env stackgres-ci; " + INSTALLATION_ID + ")"), userAgent());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "Env stackgres-ci; Ci gitlab",
      "Env stackgres-ci; Ci gitlab; Runner yang; Arch x86-64",
      "Env stackgres-offline-build",
      "E s",
  })
  void anyAcceptedValueIsAddedJustBeforeTheInstallationId(String extraMetadata) {
    System.setProperty(EXTRA_METADATA_PROPERTY, extraMetadata);
    assertTrue(userAgent().endsWith("; " + extraMetadata + "; " + INSTALLATION_ID + ")"),
        userAgent());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "Env stackgres-ci) (injected",
      "Env stackgres-ci\nEnv other",
      "Env ",
      "Env stackgres-ci;Ci gitlab",
      "Env stackgres-ci; Ci gitlab; Runner yang; Arch x86-64; One too-many",
      "Env sixty-five-characters-are-more-than-the-thirty-two-allowed-here",
      "Env stackgres_ci",
      "stackgres-ci",
  })
  void aValueThatIsNotAcceptedIsRefused(String extraMetadata) {
    System.setProperty(EXTRA_METADATA_PROPERTY, extraMetadata);
    KubernetesClient client = client();
    var exception = assertThrows(IllegalArgumentException.class,
        () -> new InstallationInfoHolder(client));
    assertTrue(exception.getMessage().contains(
        StackGresProperty.INSTALLATION_EXTRA_METADATA.getEnvironmentVariableName()),
        exception.getMessage());
  }
}
