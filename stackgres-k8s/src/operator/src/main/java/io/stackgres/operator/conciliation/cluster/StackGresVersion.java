/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Arrays;
import java.util.Map;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.StackGresContext;

public enum StackGresVersion {

  V09("0.9"),
  V091("0.9.1"),
  V092("0.9.2"),
  V093("0.9.3"),
  V094("0.9.4"),
  V10A1("1.0.0-alpha1"),
  V10A2("1.0.0-alpha2"),
  V10("1.0");

  private final String version;

  StackGresVersion(String version) {
    this.version = version;
  }

  public static <T extends CustomResource<?, ?>> StackGresVersion getClusterStackGresVersion(
      T cluster) {
    final Map<String, String> annotations = cluster.getMetadata().getAnnotations();
    return parseVersion(annotations.get(StackGresContext.VERSION_KEY));

  }

  private static StackGresVersion parseVersion(String clusterVersion) {
    String version = sanitizeVersion(clusterVersion);
    return Arrays.stream(StackGresVersion.values())
        .filter(historyVersion -> historyVersion.version.equals(version))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Invalid StackGres version " + version));
  }

  private static String sanitizeVersion(String version) {
    String snapshotSuffix = "-SNAPSHOT";
    if (version.endsWith(snapshotSuffix)) {
      return version.substring(0, version.length() - snapshotSuffix.length());
    } else {
      return version;
    }
  }

  public String getVersion() {
    return version;
  }

}
