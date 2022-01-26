/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import org.jooq.lambda.Seq;

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
  V11RC1("1.1.0-RC1"),
  V11RC2("1.1.0-RC2"),
  V11("1.1.0");

  public static final StackGresVersion LATEST = Seq.of(StackGresVersion.values())
      .findLast().orElseThrow();

  public enum StackGresMinorVersion {
    V09("0.9"),
    V10("1.0"),
    V11("1.1");

    public static final StackGresMinorVersion LATEST = Seq.of(StackGresMinorVersion.values())
        .findLast().orElseThrow();

    final String version;

    StackGresMinorVersion(String version) {
      this.version = version;
    }

    public String getVersion() {
      return version;
    }

    static StackGresMinorVersion ofVersion(String version) {
      return Stream.of(values())
          .filter(minorVersion -> version.startsWith(minorVersion.version))
          .findAny()
          .orElseThrow(() -> new IllegalArgumentException(
              "Missing minor version for version " + version));
    }
  }

  private final String version;
  private final StackGresMinorVersion minorVersion;

  StackGresVersion(String version) {
    this.version = version;
    this.minorVersion = StackGresMinorVersion.ofVersion(version);
  }

  public static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersion(
      StackGresCluster cluster) {
    return getStackGresVersionFromResource(cluster);
  }

  public static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersion(
      StackGresBackup backup) {
    return getStackGresVersionFromResource(backup);
  }

  public static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersion(
      StackGresDbOps dbOps) {
    return getStackGresVersionFromResource(dbOps);
  }

  public static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersion(
      StackGresDistributedLogs distributedLogs) {
    return getStackGresVersionFromResource(distributedLogs);
  }

  private static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersionFromResource(
      HasMetadata resource) {
    return Optional.of(resource)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresContext.VERSION_KEY))
        .map(StackGresVersion::parseVersion)
        .orElse(StackGresVersion.LATEST);
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

  public StackGresMinorVersion getMinorVersion() {
    return minorVersion;
  }

}
