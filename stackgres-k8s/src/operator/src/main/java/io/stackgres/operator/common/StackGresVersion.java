/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Arrays;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.StackGresContext;

public enum StackGresVersion {

  V09("0.9"),
  V091("0.9.1"),
  V092("0.9.2"),
  V093("0.9.3"),
  V094("0.9.4"),
  V095("0.9.5"),
  V09_LAST("0.9.5"),
  V10A1("1.0.0-alpha1"),
  V10A2("1.0.0-alpha2"),
  V10A3("1.0.0-alpha3"),
  V10A4("1.0.0-alpha4"),
  V10B1("1.0.0-beta1"),
  V10B2("1.0.0-beta2"),
  V10B3("1.0.0-beta3"),
  V10RC1("1.0.0-RC1"),
  V10("1.0.0"),
  V11B1("1.1.0-beta1"),
  V11("1.1.0");

  private final String version;

  StackGresVersion(String version) {
    this.version = version;
  }

  public static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersion(
      HasMetadata resource) {
    return parseVersion(Optional.of(resource)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresContext.VERSION_KEY))
        .orElseThrow(() -> new IllegalArgumentException(
            "Could not find required annotation " + StackGresContext.VERSION_KEY)));
  }

  private static StackGresVersion parseVersion(String clusterVersion) {
    String version = sanitizeVersion(clusterVersion);
    return Arrays.stream(StackGresVersion.values())
        .filter(historyVersion -> historyVersion.version.equals(version))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Invalid version " + version));
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
