/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.app;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.StackGresProperty;
import jakarta.ws.rs.core.HttpHeaders;

public abstract class AbstractInstallationInfoHolder {

  private static final AtomicReference<InstallationInfo> INSTALLATION_INFO =
      new AtomicReference<>();

  private static final Pattern EXTRA_METADATA_PATTERN = Pattern.compile(
      "[A-Za-z0-9-]{1,32} [A-Za-z0-9-]{1,32}(?:; [A-Za-z0-9-]{1,32} [A-Za-z0-9-]{1,32}){0,3}");

  private final KubernetesClient client;

  private final String extraMetadata;

  public AbstractInstallationInfoHolder(
      KubernetesClient client) {
    this.client = client;
    this.extraMetadata = retrieveExtraMetadata();
  }

  /**
   * The configured extra metadata followed by the separator used in the User-Agent header, or an
   * empty string when none is configured. The value becomes part of a header grammar that others
   * parse, so anything that is not one to four {@code <key> <value>} pairs is refused here instead
   * of corrupting every request sent by this installation.
   */
  private static String retrieveExtraMetadata() {
    return StackGresProperty.INSTALLATION_EXTRA_METADATA.get()
        .map(extraMetadata -> {
          if (!EXTRA_METADATA_PATTERN.matcher(extraMetadata).matches()) {
            throw new IllegalArgumentException(
                "Environment variable "
                    + StackGresProperty.INSTALLATION_EXTRA_METADATA.getEnvironmentVariableName()
                    + " must match " + EXTRA_METADATA_PATTERN.pattern()
                    + " but was: " + extraMetadata);
          }
          return extraMetadata + "; ";
        })
        .orElse("");
  }

  private InstallationInfo getInstallationInfo() {
    if (INSTALLATION_INFO.get() == null) {
      String retrievedId = retrieveInstallationId();
      InstallationInfo retrievedInfo = new InstallationInfo(
          retrievedId, client.getKubernetesVersion().getGitVersion());
      InstallationInfo updatedInfo = INSTALLATION_INFO.updateAndGet(
          info -> info != null ? info : retrievedInfo);
      updateInstallationId(updatedInfo.id());
    }

    return INSTALLATION_INFO.get();
  }

  protected abstract String retrieveInstallationId();

  protected void updateInstallationId(String id) {
  }

  public String getInstallationId() {
    return getInstallationInfo().id();
  }

  public Map.Entry<String, String> getUserAgentHeaderEntry() {
    InstallationInfo installationInfo = getInstallationInfo();
    return Map.entry(
        HttpHeaders.USER_AGENT,
        String.format(
            Locale.ROOT,
            "StackGres/%s (Java %s; Platform %s-%s; K8s %s; %s%s)",
            StackGresProperty.OPERATOR_VERSION.getString(),
            Runtime.version().toString(),
            System.getProperty("os.name"),
            System.getProperty("os.arch"),
            installationInfo.k8sVersion(),
            extraMetadata,
            installationInfo.id()));
  }

  record InstallationInfo(String id, String k8sVersion) {};
}
